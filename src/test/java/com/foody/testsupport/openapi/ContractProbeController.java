package com.foody.testsupport.openapi;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@TestComponent
@RestController
public class ContractProbeController {

	public static final String PATH = "/contract-probe";

	@PostMapping(PATH)
	public ContractProbe echo(@RequestBody ContractProbe probe) {
		return probe;
	}

}
