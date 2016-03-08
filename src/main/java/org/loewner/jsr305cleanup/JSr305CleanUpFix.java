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

public class JSr305CleanUpFix implements ICleanUpFix {

	private final CleanUpContext _context;
	private final Iterable<Annotation> _annotationsToRemove;
	private final Collection<TypeDeclaration> _nodesToAnnotateWithParameterAreNonnullByDefault;
	private final Collection<TypeDeclaration> _nodesToAnnotateWithReturnValuesAreNonnullByDefault;

	JSr305CleanUpFix(CleanUpContext context, Iterable<Annotation> annotationsToRemove,
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
		for (final TypeDeclaration node : _nodesToAnnotateWithParameterAreNonnullByDefault) {
			final ListRewrite listRewrite = rewriter.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
			final MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
			markerAnnotation.setTypeName(ast.newName("ParametersAreNonnullByDefault"));
			listRewrite.insertFirst(markerAnnotation, null);
		}
		for (final TypeDeclaration node : _nodesToAnnotateWithReturnValuesAreNonnullByDefault) {
			final ListRewrite listRewrite = rewriter.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
			final MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
			markerAnnotation.setTypeName(ast.newName("ReturnValuesAreNonnullByDefault"));
			listRewrite.insertFirst(markerAnnotation, null);
		}

		change.setEdit(rewriter.rewriteAST());
		if (!_nodesToAnnotateWithParameterAreNonnullByDefault.isEmpty()) {
			final ImportRewrite importRewrite = ImportRewrite.create(_context.getCompilationUnit(), true);
			importRewrite.addImport("javax.annotation.ParametersAreNonnullByDefault");
			change.addEdit(importRewrite.rewriteImports(progressMonitor));
		}
		if (!_nodesToAnnotateWithReturnValuesAreNonnullByDefault.isEmpty()) {
			final ImportRewrite importRewrite = ImportRewrite.create(_context.getCompilationUnit(), true);
			importRewrite.addImport("edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault");
			change.addEdit(importRewrite.rewriteImports(progressMonitor));
		}
		return change;
	}

}
