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

import com.fitnessclub.api.dto.MembershipBriefDto;
import com.fitnessclub.api.dto.MembershipPlanResponseDto;
import com.fitnessclub.api.dto.MembershipPlanUpsertRequest;
import com.fitnessclub.service.AdminMembershipPlanService;
import com.fitnessclub.service.AdminServiceCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/memberships")
@RequiredArgsConstructor
public class AdminMembershipRestController {

	private final AdminMembershipPlanService membershipPlanService;
	private final AdminServiceCatalogService catalogService;

	@GetMapping("/brief")
	public List<MembershipBriefDto> briefList() {
		return catalogService.listMembershipsBrief();
	}

	@GetMapping
	public List<MembershipPlanResponseDto> list() {
		return membershipPlanService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipPlanResponseDto create(@RequestBody MembershipPlanUpsertRequest body) {
		return membershipPlanService.create(body);
	}

	@PutMapping("/{membershipId}")
	public MembershipPlanResponseDto update(@PathVariable Integer membershipId,
			@RequestBody MembershipPlanUpsertRequest body) {
		return membershipPlanService.update(membershipId, body);
	}

	@DeleteMapping("/{membershipId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer membershipId) {
		membershipPlanService.delete(membershipId);
	}
}
