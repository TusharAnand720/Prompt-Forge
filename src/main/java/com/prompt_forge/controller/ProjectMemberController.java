package com.prompt_forge.controller;

import com.prompt_forge.dto.member.InviteMemberRequest;
import com.prompt_forge.dto.member.MemberResponse;
import com.prompt_forge.dto.member.UpdateMemberRoleRequest;
import com.prompt_forge.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/projects/{projectId}/member")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ProjectMemberController {

    ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMember(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(@PathVariable Long projectId,
                                                       @RequestBody @Valid InviteMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.inviteMember(projectId, request));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(@PathVariable Long projectId, @PathVariable Long memberId,
                                                           @RequestBody @Valid UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(projectMemberService.updateMemberRole(projectId, memberId, request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long projectId, @PathVariable Long memberId) {
        projectMemberService.removeProjectMember(projectId, memberId);
        return ResponseEntity.noContent().build();
    }
}
