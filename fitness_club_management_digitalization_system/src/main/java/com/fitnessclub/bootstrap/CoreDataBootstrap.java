package com.fitnessclub.bootstrap;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fitnessclub.domain.AdminEntity;
import com.fitnessclub.domain.Gender;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.Hall;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.AdminRepository;
import com.fitnessclub.repository.HallRepository;
import com.fitnessclub.repository.PersonalDataRepository;
import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(0)
@RequiredArgsConstructor
public class CoreDataBootstrap implements CommandLineRunner {

	private static final String SYSTEM_ADMIN_EMAIL = "internal-system-admin@noreply.local";

	private final AdminRepository adminRepository;
	private final PersonalDataRepository personalDataRepository;
	private final ZoneRepository zoneRepository;
	private final HallRepository hallRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		ensureSystemAdmin();
		ensureDefaultZones();
		normalizeHallActiveFlags();
	}

	/** Старые записи с is_active = NULL иначе не попадают в API как активные. */
	private void normalizeHallActiveFlags() {
		for (Hall h : hallRepository.findAll()) {
			if (h.getIsActive() == null) {
				h.setIsActive(Boolean.TRUE);
				hallRepository.save(h);
			}
		}
	}

	private void ensureSystemAdmin() {
		if (adminRepository.count() > 0) {
			return;
		}
		var existingPd = personalDataRepository.findByEmailIgnoreCase(SYSTEM_ADMIN_EMAIL);
		if (existingPd.isPresent()) {
			AdminEntity admin = new AdminEntity();
			admin.setPersonalDataId(existingPd.get().getPersonalDataId());
			admin.setIsActive(Boolean.TRUE);
			adminRepository.save(admin);
			return;
		}
		PersonalData pd = new PersonalData();
		pd.setLastName("Системный");
		pd.setFirstName("Администратор");
		pd.setMiddleName(null);
		pd.setBirthDate(LocalDate.of(1990, 1, 1));
		pd.setPhone("+79990000001");
		pd.setEmail(SYSTEM_ADMIN_EMAIL);
		pd.setGender(Gender.Мужской);
		pd.setPasswordHash(passwordEncoder.encode("Admin#ChangeMe1"));
		pd.setRegistrationAddress("Служебная учётная запись приложения для создания залов.");
		pd.setActualAddress(null);
		personalDataRepository.save(pd);

		AdminEntity admin = new AdminEntity();
		admin.setPersonalDataId(pd.getPersonalDataId());
		admin.setIsActive(Boolean.TRUE);
		adminRepository.save(admin);
	}

	private void ensureDefaultZones() {
		if (zoneRepository.count() > 0) {
			return;
		}
		Zone z1 = new Zone();
		z1.setZoneName("Functional");
		z1.setDescription("Силовые и функциональные тренировки.");
		zoneRepository.save(z1);
		Zone z2 = new Zone();
		z2.setZoneName("Mind & Body");
		z2.setDescription("Йога, растяжка.");
		zoneRepository.save(z2);
	}
}
