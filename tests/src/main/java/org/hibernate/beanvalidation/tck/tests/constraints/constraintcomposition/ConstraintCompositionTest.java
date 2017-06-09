/**
 * Bean Validation TCK
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.beanvalidation.tck.tests.constraints.constraintcomposition;

import static org.hibernate.beanvalidation.tck.util.ConstraintViolationAssert.assertConstraintViolation;
import static org.hibernate.beanvalidation.tck.util.ConstraintViolationAssert.assertCorrectConstraintTypes;
import static org.hibernate.beanvalidation.tck.util.ConstraintViolationAssert.assertCorrectConstraintViolationMessages;
import static org.hibernate.beanvalidation.tck.util.ConstraintViolationAssert.assertNumberOfViolations;
import static org.hibernate.beanvalidation.tck.util.ConstraintViolationAssert.pathWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.UnexpectedTypeException;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import org.hibernate.beanvalidation.tck.beanvalidation.Sections;
import org.hibernate.beanvalidation.tck.tests.AbstractTCKTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecVersion;
import org.testng.annotations.Test;

/**
 * Tests for composing constraints.
 *
 * @author Hardy Ferentschik
 * @author Guillaume Smet
 */
@SpecVersion(spec = "beanvalidation", version = "2.0.0")
public class ConstraintCompositionTest extends AbstractTCKTest {

	@Deployment
	public static WebArchive createTestArchive() {
		return webArchiveBuilder()
				.withTestClassPackage( ConstraintCompositionTest.class )
				.build();
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "a")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "q")
	public void testComposedConstraints() {
		FrenchAddress address = getFrenchAddressWithoutZipCode();
		Set<ConstraintViolation<FrenchAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 1 );
		ConstraintViolation<FrenchAddress> constraintViolation = constraintViolations.iterator().next();
		assertCorrectConstraintTypes( constraintViolations, NotNull.class );
		assertCorrectConstraintViolationMessages( constraintViolations, "may not be null" );
		assertConstraintViolation(
				constraintViolation,
				FrenchAddress.class,
				null,
				pathWith().property( "zipCode" )
		);
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "a")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "q")
	public void testComposedConstraintsAreRecursive() {
		GermanAddress address = new GermanAddress();
		address.setAddressline1( "Rathausstrasse 5" );
		address.setAddressline2( "3ter Stock" );
		address.setCity( "Karlsruhe" );
		Set<ConstraintViolation<GermanAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 1 );
		assertConstraintViolation(
				constraintViolations.iterator().next(),
				GermanAddress.class,
				null,
				pathWith().property( "zipCode" )
		);
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "b")
	public void testValidationOfMainAnnotationIsAlsoApplied() {
		FrenchAddress address = getFrenchAddressWithoutZipCode();
		address.setZipCode( "00000" );
		Set<ConstraintViolation<FrenchAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 1 );
		assertCorrectConstraintTypes( constraintViolations, FrenchZipcode.class );
		assertCorrectConstraintViolationMessages( constraintViolations, "00000 is a reserved code" );
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "c")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "n")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "r")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "s")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "v")
	public void testEachFailingConstraintCreatesConstraintViolation() {
		FrenchAddress address = getFrenchAddressWithoutZipCode();
		address.setZipCode( "abc" );
		Set<ConstraintViolation<FrenchAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 3 );
		assertCorrectConstraintTypes( constraintViolations, Pattern.class, Pattern.class, Size.class );
		for ( ConstraintViolation<FrenchAddress> violation : constraintViolations ) {
			assertConstraintViolation(
					violation,
					FrenchAddress.class,
					"abc",
					pathWith().property( "zipCode" )
			);
		}

		address.setZipCode( "123" );
		constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 2 );
		assertCorrectConstraintTypes( constraintViolations, Pattern.class, Size.class );
		for ( ConstraintViolation<FrenchAddress> violation : constraintViolations ) {
			assertConstraintViolation(
					violation,
					FrenchAddress.class,
					"123",
					pathWith().property( "zipCode" )
			);
		}

		address.setZipCode( "33023" );
		constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 0 );
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "t")
	public void testConstraintIndexWithListContainer() {
		FrenchAddressListContainer address = getFrenchAddressListContainerWithoutZipCode();
		address.setZipCode( "abc" );
		Set<ConstraintViolation<FrenchAddressListContainer>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 3 );
		assertCorrectConstraintTypes( constraintViolations, Pattern.class, Pattern.class, Size.class );
		for ( ConstraintViolation<FrenchAddressListContainer> violation : constraintViolations ) {
			assertConstraintViolation(
					violation,
					FrenchAddressListContainer.class,
					"abc",
					pathWith().property( "zipCode" )
			);
		}

		address.setZipCode( "33023" );
		constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 0 );
	}

	@Test(expectedExceptions = { ConstraintDeclarationException.class })
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "u")
	public void testConstraintIndexWithMixDirectAnnotationAndListContainer() {
		FrenchAddressMixDirectAnnotationAndListContainer address = getFrenchAddressMixDirectAnnotationAndListContainerWithoutZipCode();
		address.setZipCode( "abc" );
		getValidator().validate( address );
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "d")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "e")
	public void testGroupsDefinedOnMainAnnotationAreInherited() {
		FrenchAddress address = getFrenchAddressWithoutZipCode();
		Set<ConstraintViolation<FrenchAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 1 );
		ConstraintViolation<FrenchAddress> constraintViolation = constraintViolations.iterator().next();
		assertCorrectConstraintTypes( constraintViolations, NotNull.class );
		NotNull notNull = (NotNull) constraintViolation.getConstraintDescriptor().getAnnotation();
		List<Class<?>> groups = Arrays.asList( notNull.groups() );
		assertTrue( groups.size() == 2, "There should be two groups" );
		assertTrue( groups.contains( Default.class ), "The default group should be in the list." );
		assertTrue(
				groups.contains( FrenchAddress.FullAddressCheck.class ),
				"The FrenchAddress.FullAddressCheck group should be inherited."
		);
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "l")
	public void testOnlySingleConstraintViolation() {
		GermanAddress address = new GermanAddress();
		address.setAddressline1( "Rathausstrasse 5" );
		address.setAddressline2( "3ter Stock" );
		address.setCity( "Karlsruhe" );
		address.setZipCode( "abc" );
		// actually three composing constraints fail, but due to @ReportAsSingleViolation only one will be reported.
		Set<ConstraintViolation<GermanAddress>> constraintViolations = getValidator().validate( address );
		assertNumberOfViolations( constraintViolations, 1 );
		assertConstraintViolation(
				constraintViolations.iterator().next(),
				GermanAddress.class,
				"abc",
				pathWith().property( "zipCode" )
		);
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "m")
	public void testAttributesDefinedOnComposingConstraints() {
		BeanDescriptor descriptor = getValidator().getConstraintsForClass( FrenchAddress.class );
		Set<ConstraintDescriptor<?>> constraintDescriptors = descriptor.getConstraintsForProperty( "zipCode" )
				.getConstraintDescriptors();
		boolean findPattern = checkForAppropriateAnnotation( constraintDescriptors );
		assertTrue( findPattern, "Could not find @Pattern in composing constraints" );
	}

	private boolean checkForAppropriateAnnotation(Set<ConstraintDescriptor<?>> constraintDescriptors) {
		boolean findPattern = false;
		for ( ConstraintDescriptor<?> constraintDescriptor : constraintDescriptors ) {
			Annotation ann = constraintDescriptor.getAnnotation();
			if ( Pattern.class.getName().equals( ann.annotationType().getName() ) ) {
				String regexp = ( (Pattern) ann ).regexp();
				if ( regexp.equals( "bar" ) ) {
					fail( "The regular expression attributes are defined in the composing constraint." );
				}
				findPattern = true;
			}
			findPattern |= checkForAppropriateAnnotation( constraintDescriptor.getComposingConstraints() );
		}
		return findPattern;
	}

	@Test(expectedExceptions = ConstraintDefinitionException.class)
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "p")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "w")
	public void testOverriddenAttributesMustMatchInType() {
		getValidator().validate( new DummyEntityWithZipCode( "foobar" ) );
	}

	@Test(expectedExceptions = UnexpectedTypeException.class)
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "j")
	public void testAllComposingConstraintsMustBeApplicableToAnnotatedType() {
		getValidator().validate( new Shoe( 41 ) );
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "f")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "g")
	public void testPayloadPropagationInComposedConstraints() {
		Friend john = new Friend( "John", "Doe" );

		Set<ConstraintViolation<Friend>> constraintViolations = getValidator().validate( john );

		assertNumberOfViolations( constraintViolations, 1 );
		assertCorrectConstraintTypes( constraintViolations, NotNull.class );

		ConstraintViolation<Friend> constraintViolation = constraintViolations.iterator().next();
		Set<Class<? extends Payload>> payloads = constraintViolation.getConstraintDescriptor().getPayload();

		assertTrue( payloads.size() == 1, "There should be one payload in the set" );
		Class<? extends Payload> payload = payloads.iterator().next();
		assertTrue( payload.getName().equals( Severity.Warn.class.getName() ), "Unexpected payload" );
	}

	@Test
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "h")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "i")
	public void testConstraintTargetPropagationInComposedConstraints() throws Exception {
		Object object = new DummyEntityWithGenericAndCrossParameterConstraint();
		Method method = DummyEntityWithGenericAndCrossParameterConstraint.class.getMethod( "doSomething", int.class );
		Object[] parameterValues = new Object[] { 0 };

		Set<ConstraintViolation<Object>> constraintViolations = getExecutableValidator().validateParameters(
				object,
				method,
				parameterValues
		);

		//The composing constraint is expected to fail
		assertNumberOfViolations( constraintViolations, 1 );
		assertCorrectConstraintTypes( constraintViolations, GenericAndCrossParameterConstraint.class );

		//and it should inherit the constraint target from the composed constraint
		ConstraintViolation<Object> constraintViolation = constraintViolations.iterator().next();
		assertEquals(
				constraintViolation.getConstraintDescriptor().getValidationAppliesTo(),
				ConstraintTarget.PARAMETERS
		);
	}

	@Test(expectedExceptions = ConstraintDefinitionException.class)
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "k")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "w")
	public void testMixedConstraintTargetsInComposedAndComposingConstraintsCauseException()
			throws Exception {
		Object object = new DummyEntityWithIllegallyComposedConstraint();
		Method method = DummyEntityWithIllegallyComposedConstraint.class.getMethod(
				"doSomething",
				int.class
		);
		Object[] parameterValues = new Object[0];

		getExecutableValidator().validateParameters(
				object,
				method,
				parameterValues
		);
	}

	@Test(expectedExceptions = ConstraintDefinitionException.class)
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "k")
	@SpecAssertion(section = Sections.CONSTRAINTSDEFINITIONIMPLEMENTATION_CONSTRAINTCOMPOSITION, id = "w")
	public void testMixedConstraintTargetsInComposingConstraintsCauseException() throws Exception {
		Object object = new DummyEntityWithAnotherIllegallyComposedConstraint();
		Method method = DummyEntityWithAnotherIllegallyComposedConstraint.class.getMethod(
				"doSomething",
				int.class
		);
		Object[] parameterValues = new Object[0];

		getExecutableValidator().validateParameters(
				object,
				method,
				parameterValues
		);
	}

	private FrenchAddress getFrenchAddressWithoutZipCode() {
		FrenchAddress address = new FrenchAddress();
		address.setAddressline1( "10 rue des Treuils" );
		address.setAddressline2( "BP 12 " );
		address.setCity( "Bordeaux" );
		return address;
	}

	private FrenchAddressListContainer getFrenchAddressListContainerWithoutZipCode() {
		FrenchAddressListContainer address = new FrenchAddressListContainer();
		address.setAddressline1( "10 rue des Treuils" );
		address.setAddressline2( "BP 12 " );
		address.setCity( "Bordeaux" );
		return address;
	}

	private FrenchAddressMixDirectAnnotationAndListContainer getFrenchAddressMixDirectAnnotationAndListContainerWithoutZipCode() {
		FrenchAddressMixDirectAnnotationAndListContainer address = new FrenchAddressMixDirectAnnotationAndListContainer();
		address.setAddressline1( "10 rue des Treuils" );
		address.setAddressline2( "BP 12 " );
		address.setCity( "Bordeaux" );
		return address;
	}

	private static class DummyEntityWithZipCode {
		@FrenchZipcodeWithInvalidOverride
		String zip;

		DummyEntityWithZipCode(String zip) {
			this.zip = zip;
		}
	}

	private static class DummyEntityWithGenericAndCrossParameterConstraint {
		@ComposedGenericAndCrossParameterConstraint(validationAppliesTo = ConstraintTarget.PARAMETERS)
		public Object doSomething(int i) {
			return null;
		}
	}

	private static class DummyEntityWithIllegallyComposedConstraint {
		@ParametersNotEmpty
		public Object doSomething(int i) {
			return null;
		}
	}

	private static class DummyEntityWithAnotherIllegallyComposedConstraint {
		@ComposedConstraint
		public void doSomething(int i) {
		}
	}
}
