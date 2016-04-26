package org.loewner.jsr305cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class Jsr305CleanUp implements ICleanUp {

	static final String USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT = "org.loewner.jsr305cleanup.USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT";
	static final String USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT = "org.loewner.jsr305cleanup.USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT";

	static final String FQN_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT = "edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault";
	static final String FQN_PARAMETERS_ARE_NONNULL_BY_DEFAULT = "javax.annotation.ParametersAreNonnullByDefault";

	private CleanUpOptions _options;

	@Override
	public void setOptions(CleanUpOptions options) {
		_options = options;

	}

	@Override
	public String[] getStepDescriptions() {
		final boolean parameterAreNonnullByDefault = _options.isEnabled(USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT);
		final boolean returnValuesAreNonnullByDefault = _options.isEnabled(USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT);
		final List<String> steps = new ArrayList<>();
		if (parameterAreNonnullByDefault) {
			steps.add("Replacing @Nonnull parameter annotations with @ParametersAreNonnullByDefault");
		}
		if (returnValuesAreNonnullByDefault) {
			steps.add("Replacing @Nonnnull return value annotations with @ReturnValuesAreNonnullByDefault");
		}
		return steps.toArray(new String[0]);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(true, true, false, null);
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		final boolean parameterAreNonnullByDefault = _options.isEnabled(USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT);
		final boolean returnValuesAreNonnullByDefault = _options.isEnabled(USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT);
		if (!parameterAreNonnullByDefault && !returnValuesAreNonnullByDefault) {
			return null;
		}
		final Collection<Annotation> annotationsToRemove = new ArrayList<>();
		final Collection<TypeDeclaration> nodesToAnnotateWithParameterAreNonnullByDefault = new HashSet<>();
		final Collection<TypeDeclaration> nodesToAnnotateWithReturnValuesAreNonnullByDefault = new HashSet<>();
		context.getAST().accept(new ASTVisitor() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean visit(MethodDeclaration node) {
				final boolean overridden = isOverridden(node);
				if (!overridden && node.getParent() instanceof TypeDeclaration) {
					final TypeDeclaration typeDecl = (TypeDeclaration) node.getParent();
					if (returnValuesAreNonnullByDefault) {
						final Annotation anno = getNonnullReturnValueAnnotationIfPresent(node);
						if (anno != null) {
							annotationsToRemove.add(anno);
							if (!hasAnnotation(typeDecl, Jsr305CleanUp.FQN_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT)) {
								nodesToAnnotateWithReturnValuesAreNonnullByDefault.add(typeDecl);
							}
						}
					}
					if (parameterAreNonnullByDefault) {
						final Collection<Annotation> annos = getNonnullAnnotationsOnParameters(node.parameters());
						if (!annos.isEmpty()) {
							annotationsToRemove.addAll(annos);
							if (!hasAnnotation(typeDecl, Jsr305CleanUp.FQN_PARAMETERS_ARE_NONNULL_BY_DEFAULT)) {
								nodesToAnnotateWithParameterAreNonnullByDefault.add(typeDecl);
							}
						}
					}
				}
				return false;
			}

		});
		if (annotationsToRemove.isEmpty()) {
			return null;
		}
		return new Jsr305CleanUpFix(context, annotationsToRemove, nodesToAnnotateWithParameterAreNonnullByDefault,
				nodesToAnnotateWithReturnValuesAreNonnullByDefault);
	}

	protected boolean isOverridden(MethodDeclaration node) {
		final IMethodBinding methodBinding = node.resolveBinding();
		final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
		return isDefinedInTypeOrSuperType(methodBinding, declaringClass);
	}

	private boolean isDefinedInTypeOrSuperType(final IMethodBinding methodBinding, ITypeBinding declaringClass) {
		while (declaringClass != null) {
			for (final IMethodBinding method : declaringClass.getDeclaredMethods()) {
				if (methodBinding.overrides(method)) {
					return true;
				}
			}
			for (final ITypeBinding type : declaringClass.getInterfaces()) {
				if (isDefinedInTypeOrSuperType(methodBinding, type)) {
					return true;
				}
			}
			declaringClass = declaringClass.getSuperclass();
		}
		return false;
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	private static Annotation getNonnullReturnValueAnnotationIfPresent(MethodDeclaration node) {
		@SuppressWarnings("unchecked")
		final List<IExtendedModifier> modifiers = node.modifiers();
		return findNonnullAnnotation(modifiers);
	}

	@SuppressWarnings("unchecked")
	private static Collection<Annotation> getNonnullAnnotationsOnParameters(
			List<SingleVariableDeclaration> parameters) {
		final Collection<Annotation> result = new ArrayList<>();
		for (final SingleVariableDeclaration decl : parameters) {
			final Annotation anno = findNonnullAnnotation(decl.modifiers());
			if (anno != null) {
				result.add(anno);
			}
		}
		return result;
	}

	private static Annotation findNonnullAnnotation(final List<IExtendedModifier> modifiers) {
		final String className = "javax.annotation.Nonnull";
		return findAnnotation(modifiers, className);
	}

	@SuppressWarnings("unchecked")
	private boolean hasAnnotation(TypeDeclaration typeDecl, String className) {
		return findAnnotation(typeDecl.modifiers(), className) != null;
	}

	private static Annotation findAnnotation(final List<IExtendedModifier> modifiers, final String className) {
		for (final IExtendedModifier modifier : modifiers) {
			if (modifier.isAnnotation()) {
				final Annotation anno = (Annotation) modifier;
				final String fqn = anno.resolveTypeBinding().getQualifiedName();
				if (className.equals(fqn)) {
					return anno;
				}
			}
		}
		return null;
	}

}
