package com.foody.api.architecture;

public final class ArchitectureConstants {

	public static final String BASE_PACKAGE = "com.foody.api";

	public static final String CONTROLLER_LAYER = "Controller";

	public static final String SERVICE_LAYER = "Service";

	public static final String REPOSITORY_LAYER = "Repository";

	public static final String CONTROLLER_PACKAGE = "..controller..";

	public static final String SERVICE_PACKAGE = "..service..";

	public static final String REPOSITORY_PACKAGE = "..repository..";

	public static final String AUTH_CONTROLLER_PACKAGE = "..auth.controller..";

	public static final String FEATURE_SLICES = BASE_PACKAGE + ".(*)..";

	public static final String SHARED_SLICES = BASE_PACKAGE + ".shared.(*)..";

	private ArchitectureConstants() {
	}

}
