package com.fitnessclub;

import java.time.ZoneId;

/**
 * Часовой пояс клуба (Самара, UTC+4, без перехода на летнее время).
 * Используется JVM по умолчанию, Jackson и Hibernate JDBC.
 * На страницах отображение дат в этом же поясе — {@code static/js/club-datetime.js} (см. {@code fcFormatDateTime}).
 */
public final class ClubZone {

	public static final ZoneId ZONE = ZoneId.of("Europe/Samara");
	public static final String ZONE_ID = ZONE.getId();

	private ClubZone() {
	}
}
