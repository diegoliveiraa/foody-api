package com.foody.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.foody.api.shared.exception.GlobalExceptionHandler;
import com.foody.api.shared.persistence.BaseEntity;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = ArchitectureConstants.BASE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule layersOnlyCallDownwards = layeredArchitecture().consideringOnlyDependenciesInLayers()
		.withOptionalLayers(true)
		.layer(ArchitectureConstants.CONTROLLER_LAYER)
		.definedBy(ArchitectureConstants.CONTROLLER_PACKAGE)
		.layer(ArchitectureConstants.SERVICE_LAYER)
		.definedBy(ArchitectureConstants.SERVICE_PACKAGE)
		.layer(ArchitectureConstants.REPOSITORY_LAYER)
		.definedBy(ArchitectureConstants.REPOSITORY_PACKAGE)
		.whereLayer(ArchitectureConstants.CONTROLLER_LAYER)
		.mayNotBeAccessedByAnyLayer()
		.whereLayer(ArchitectureConstants.SERVICE_LAYER)
		.mayOnlyBeAccessedByLayers(ArchitectureConstants.CONTROLLER_LAYER, ArchitectureConstants.SERVICE_LAYER)
		.whereLayer(ArchitectureConstants.REPOSITORY_LAYER)
		.mayOnlyBeAccessedByLayers(ArchitectureConstants.SERVICE_LAYER);

	@ArchTest
	static final ArchRule servicesNeverTouchTheDatabaseDirectly = noClasses().that()
		.resideInAPackage(ArchitectureConstants.SERVICE_PACKAGE)
		.should()
		.dependOnClassesThat()
		.belongToAnyOf(EntityManager.class, JdbcTemplate.class, NamedParameterJdbcTemplate.class, DataSource.class)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersNeverSeeEntities = noClasses().that()
		.areAnnotatedWith(RestController.class)
		.should()
		.dependOnClassesThat()
		.areAnnotatedWith(Entity.class)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule everyEntityExtendsBaseEntity = classes().that()
		.areAnnotatedWith(Entity.class)
		.should()
		.beAssignableTo(BaseEntity.class)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersLiveInControllerPackages = classes().that()
		.areAnnotatedWith(RestController.class)
		.should()
		.resideInAPackage(ArchitectureConstants.CONTROLLER_PACKAGE)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule servicesLiveInServicePackages = classes().that()
		.areAnnotatedWith(Service.class)
		.should()
		.resideInAPackage(ArchitectureConstants.SERVICE_PACKAGE)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule repositoriesLiveInRepositoryPackages = classes().that()
		.areInterfaces()
		.and()
		.areAssignableTo(Repository.class)
		.should()
		.resideInAPackage(ArchitectureConstants.REPOSITORY_PACKAGE)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule onlyTheGlobalHandlerHandlesExceptions = noMethods().that()
		.areDeclaredInClassesThat()
		.doNotHaveFullyQualifiedName(GlobalExceptionHandler.class.getName())
		.should()
		.beAnnotatedWith(ExceptionHandler.class);

	@ArchTest
	static final ArchRule everyPublicControllerMethodOutsideAuthIsProtected = methods().that()
		.arePublic()
		.and()
		.areDeclaredInClassesThat()
		.areAnnotatedWith(RestController.class)
		.and()
		.areDeclaredInClassesThat()
		.resideOutsideOfPackage(ArchitectureConstants.AUTH_CONTROLLER_PACKAGE)
		.should()
		.beMetaAnnotatedWith(PreAuthorize.class)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

	@ArchTest
	static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

	@ArchTest
	static final ArchRule noJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

	@ArchTest
	static final ArchRule noGenericExceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

}
