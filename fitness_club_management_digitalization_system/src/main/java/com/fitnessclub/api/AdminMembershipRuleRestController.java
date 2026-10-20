package com.fitnessclub.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.api.dto.MembershipServiceRuleResponseDto;
import com.fitnessclub.api.dto.MembershipServiceRuleUpsertRequest;
import com.fitnessclub.service.AdminMembershipRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/membership-rules")
@RequiredArgsConstructor
public class AdminMembershipRuleRestController {

	private final AdminMembershipRuleService ruleService;

	@GetMapping
	public List<MembershipServiceRuleResponseDto> list() {
		return ruleService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipServiceRuleResponseDto createOrMerge(@RequestBody MembershipServiceRuleUpsertRequest body) {
		return ruleService.upsert(body);
	}

	@PutMapping("/{membershipServiceId}")
	public MembershipServiceRuleResponseDto update(@PathVariable Integer membershipServiceId,
			@RequestBody MembershipServiceRuleUpsertRequest body) {
		return ruleService.update(membershipServiceId, body);
	}

	@DeleteMapping("/{membershipServiceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer membershipServiceId) {
		ruleService.delete(membershipServiceId);
	}
}
