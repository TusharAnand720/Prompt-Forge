package com.prompt_forge.service;

import com.prompt_forge.dto.member.InviteMemberRequest;
import com.prompt_forge.dto.member.MemberResponse;
import com.prompt_forge.dto.member.UpdateMemberRoleRequest;

import java.util.List;

public interface ProjectMemberService {

    List<MemberResponse> getProjectMembers(Long projectId);

    MemberResponse inviteMember(Long projectId, InviteMemberRequest request);

    MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request);

    void removeProjectMember(Long projectId, Long memberId);
}
