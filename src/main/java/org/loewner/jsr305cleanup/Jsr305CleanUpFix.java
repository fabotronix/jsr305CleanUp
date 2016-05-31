package org.loewner.jsr305cleanup;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class Jsr305CleanUpFix implements ICleanUpFix {

	private final CleanUpContext _context;
	private final Iterable<Annotation> _annotationsToRemove;
	private final Collection<TypeDeclaration> _nodesToAnnotateWithParameterAreNonnullByDefault;
	private final Collection<TypeDeclaration> _nodesToAnnotateWithReturnValuesAreNonnullByDefault;

	Jsr305CleanUpFix(CleanUpContext context, Iterable<Annotation> annotationsToRemove,
			Collection<TypeDeclaration> nodesToAnnotateWithParameterAreNonnullByDefault,
			Collection<TypeDeclaration> nodesToAnnotateWithReturnValuesAreNonnullByDefault) {
		_context = context;
		_annotationsToRemove = annotationsToRemove;
		_nodesToAnnotateWithParameterAreNonnullByDefault = nodesToAnnotateWithParameterAreNonnullByDefault;
		_nodesToAnnotateWithReturnValuesAreNonnullByDefault = nodesToAnnotateWithReturnValuesAreNonnullByDefault;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		final CompilationUnitChange change = new CompilationUnitChange("Remove unnecessary visibility modifiers",
				_context.getCompilationUnit());
		final AST ast = _context.getAST().getAST();
		final ASTRewrite rewriter = ASTRewrite.create(ast);
		for (final ASTNode node : _annotationsToRemove) {
			rewriter.remove(node, null);
		}
		addAnnotation(ast, rewriter, "ParametersAreNonnullByDefault", _nodesToAnnotateWithParameterAreNonnullByDefault);
		addAnnotation(ast, rewriter, "ReturnValuesAreNonnullByDefault",
				_nodesToAnnotateWithReturnValuesAreNonnullByDefault);

		change.setEdit(rewriter.rewriteAST());
		final boolean addImportForParameters = !_nodesToAnnotateWithParameterAreNonnullByDefault.isEmpty();
		final boolean addImportForReturns = !_nodesToAnnotateWithReturnValuesAreNonnullByDefault.isEmpty();
		if (addImportForParameters || addImportForReturns) {
			final ImportRewrite importRewrite = ImportRewrite.create(_context.getCompilationUnit(), true);
			if (addImportForParameters) {
				importRewrite.addImport(Jsr305CleanUp.FQN_PARAMETERS_ARE_NONNULL_BY_DEFAULT);
			}
			if (addImportForReturns) {
				importRewrite.addImport(Jsr305CleanUp.FQN_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT);
			}
			change.addEdit(importRewrite.rewriteImports(progressMonitor));
		}
		return change;
	}

	private static void addAnnotation(final AST ast, final ASTRewrite rewriter, String annotationName,
			Collection<TypeDeclaration> nodes) {
		for (final TypeDeclaration node : nodes) {
			final ListRewrite listRewrite = rewriter.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
			final MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
			markerAnnotation.setTypeName(ast.newName(annotationName));
			listRewrite.insertFirst(markerAnnotation, null);
		}
	}

}
