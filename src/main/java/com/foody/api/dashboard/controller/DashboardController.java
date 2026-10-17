package com.foody.api.dashboard.controller;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.dashboard.dto.DashboardPeriod;
import com.foody.api.dashboard.dto.DashboardResponse;
import com.foody.api.dashboard.service.DashboardService;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DashboardConstants.DASHBOARD_PATH)
public class DashboardController {

	private final DashboardService dashboard;

	public DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping
	@RequiresScreen(Screen.DASHBOARD_OVERVIEW)
	public DashboardResponse overview(
			@RequestParam(defaultValue = DashboardConstants.DEFAULT_PERIOD) DashboardPeriod period) {
		return dashboard.overview(period);
	}

}
