package com.prompt_forge.service.impl;

import com.prompt_forge.dto.deploy.DeployResponse;
import com.prompt_forge.service.DeploymentService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesDeploymentServiceImpl implements DeploymentService {

    private static final String NAMESPACE = "promptforge-apps";
    private static final String POOL_LABEL = "status";
    private static final String PROJECT_LABEL = "project-id";
    private static final String IDLE = "idle";
    private static final String BUSY = "busy";
    private static final String SYNCER_CONTAINER = "syncer";
    private static final String RUNNER_CONTAINER = "runner";
    private static final String REVERSE_PROXY_PORT = "8090";

    private final KubernetesClient kubernetesClient;

    @Override
    public DeployResponse deploy(Long projectId) {

        String domain = "project-" + projectId + ".app.domain.com";

        Pod existingPod = findActivePod(projectId);

        if (existingPod != null) {
            log.info("Found existing active pod for project {}: {}", projectId, existingPod.getMetadata().getName());
            return new DeployResponse("http://" + domain + ":" + REVERSE_PROXY_PORT);
        }


        return claimAndStartPod(projectId, domain);
    }

    private DeployResponse claimAndStartPod(Long projectId, String domain) {

        try {
            Pod idlePod = kubernetesClient.pods()
                    .inNamespace(NAMESPACE)
                    .withLabel(POOL_LABEL, IDLE)
                    .list().getItems().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No idle pods available for deployment"));

            String podName = idlePod.getMetadata().getName();
            log.info("Claiming idle pod {} for project {}", podName, projectId);

            kubernetesClient.pods().inNamespace(NAMESPACE)
                    .withName(podName)
                    .edit(pod -> {
                        pod.getMetadata().getLabels().put(PROJECT_LABEL, String.valueOf(projectId));
                        pod.getMetadata().getLabels().put(POOL_LABEL, BUSY);
                        return pod;
                    });

            // SYNCER CONTAINER: Initial sync and start watching for changes
            {
                String initialSyncCmd = String.format(
                        "mc mirror --overwrite myminio/projects/%d/ /app/",
                        projectId
                );
                log.info("Starting initial sync for project {} in syncer container of pod {}: {}", projectId, podName, initialSyncCmd);
                execCommand(podName, SYNCER_CONTAINER, "sh", "-c", initialSyncCmd);

                String watchCmd = String.format(
                        "nohup mc mirror --overwrite --watch myminio/projects/%d/ /app/ > /app/sync.log 2>&7 &",
                        projectId
                );
                log.info("Starting watch for project {} in syncer container of pod {}: {}", projectId, podName, watchCmd);
                execCommand(podName, SYNCER_CONTAINER, "sh", "-c", watchCmd);
            }

            // RUNNER CONTAINER: Start the application
            {
                String startAppCmd = "npm install && nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";
                log.info("Starting project application {} in runner container of pod {}: {}", projectId, podName, startAppCmd);
                execCommand(podName, RUNNER_CONTAINER, "sh", "-c", startAppCmd);
            }
            return new DeployResponse("http://" + domain + ":" + REVERSE_PROXY_PORT);
        } catch (Exception e) {
            log.error("Error during deployment of project {}: {}", projectId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void execCommand(String podName, String container, String... commands) {

        log.info("Executing {} commands on pod {}", Arrays.asList(commands), podName);

        CompletableFuture<String> data = new CompletableFuture<>();
        try (ExecWatch ignored = kubernetesClient.pods().inNamespace(NAMESPACE)
                .withName(podName)
                .inContainer(container)
                .writingOutput(new ByteArrayOutputStream())
                .writingError(new ByteArrayOutputStream())
                .usingListener(new ExecListener() {
                    @Override
                    public void onClose(int code, String reason) {
                        data.complete("Done");
                    }
                })
                .exec(commands)) {

            if (commands[commands.length - 1].trim().endsWith("&")) {
                Thread.sleep(500);
            } else {
                data.get(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Error executing command in pod {}: {}", podName, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Pod findActivePod(Long projectId) {
        return kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withLabel(PROJECT_LABEL, String.valueOf(projectId))
                .withLabel(POOL_LABEL, BUSY)
                .list().getItems().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .findFirst()
                .orElse(null);
    }
}
