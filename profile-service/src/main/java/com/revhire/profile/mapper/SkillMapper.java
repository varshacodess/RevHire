package com.revhire.profile.mapper;

import com.revhire.profile.dto.SkillRequest;
import com.revhire.profile.dto.SkillResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Skill;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {
    public Skill toEntity(CandidateProfile profile, SkillRequest request) {
        return new Skill(profile, request.name().trim(), request.proficiency());
    }

    public void update(Skill skill, SkillRequest request) {
        skill.update(request.name().trim(), request.proficiency());
    }

    public SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getProficiency());
    }
}
