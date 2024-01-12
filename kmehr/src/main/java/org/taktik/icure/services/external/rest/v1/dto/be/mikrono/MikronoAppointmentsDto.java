/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.mikrono;

import java.util.List;

public class MikronoAppointmentsDto {
	private List<MikronoAppointmentDto> appointments;

	public List<MikronoAppointmentDto> getAppointments() {
		return appointments;
	}

	public void setAppointments(List<MikronoAppointmentDto> appointments) {
		this.appointments = appointments;
	}
}
