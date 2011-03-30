package de.sebastianbenz.task.query;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;

import java.util.Collections;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.PolymorphicDispatcher;
import org.eclipse.xtext.util.SimpleCache;
import org.eclipse.xtext.util.Tuples;

import com.google.common.base.Function;

import de.sebastianbenz.task.Container;
import de.sebastianbenz.task.Content;
import de.sebastianbenz.task.Note;
import de.sebastianbenz.task.Project;
import de.sebastianbenz.task.Tag;
import de.sebastianbenz.task.Task;
import de.sebastianbenz.task.util.Numbers;

public class QueryInterpreter {

	private PolymorphicDispatcher<Boolean> selectDispatcher = PolymorphicDispatcher
			.createForSingleTarget("_select", 2, 2, this);

	
	private SimpleCache<Pair<Expression, EObject>, Boolean> selectionCache = new SimpleCache<Pair<Expression,EObject>, Boolean>(new Function<Pair<Expression, EObject>, Boolean>() {
		public Boolean apply(Pair<Expression, EObject> args) {
			return selectDispatcher.invoke(args.getFirst(), args.getSecond());
		}
	});
	
	
	public boolean select(Query query, EObject element) {
		if (query == null || element == null) {
			return true;
		}
		if (query.getExpression() == null) {
			return true;
		}
		if (element instanceof Project) {
			for (Content content : allContents((Project) element,
					Collections.<Content> emptyList())) {
				if (internalSelect(query.getExpression(), content)) {
					return true;
				}
			}
		}
		return internalSelect(query.getExpression(), element);
	}

	private Iterable<Content> allContents(Project project,
			Iterable<Content> contents) {
		EList<Content> children = project.getChildren();
		contents = concat(contents, filter(children, Note.class));
		contents = concat(contents, filter(children, Task.class));
		for (Project subProject : filter(children, Project.class)) {
			contents = concat(contents, allContents(subProject, contents));
		}
		return contents;
	}

	protected boolean internalSelect(Expression expr, EObject target) {
		if (expr == null || target == null) {
			return true;
		}
		return selectionCache.get(Tuples.create(expr, target));
	}

	protected boolean _select(EObject expr, EObject object) {
		return false;
	}

	protected boolean _select(AndExpr andExpr, EObject object) {
		return internalSelect(andExpr.getLeft(), object)
				&& internalSelect(andExpr.getRight(), object);
	}

	protected boolean _select(ParenExpr expr, EObject object) {
		return internalSelect(expr.getExpr(), object);
	}

	protected boolean _select(OrExpr expr, EObject object) {
		return internalSelect(expr.getLeft(), object)
				|| internalSelect(expr.getRight(), object);
	}

	protected boolean _select(CompEqExpr expr, Task task) {
		if (isNotValue(expr.getLeft())) {
			return false;
		}
		if (isNotValue(expr.getRight())) {
			return false;
		}
		String left = valueOf((Value) expr.getLeft(), task);
		String right = valueOf((Value) expr.getRight(), task);
		
		if(left == null || right == null){
			return false;
		}
		
		boolean isEqual = left.equals(right);

		if (expr.getOperator() == CompEqOperator.EQUAL) {
			return isEqual;
		} else {
			return !isEqual;
		}
	}

	protected boolean _select(CompExpr expr, Task task) {
		if (isNotValue(expr.getLeft())) {
			return false;
		}
		if (isNotValue(expr.getRight())) {
			return false;
		}
		Number left = asNumber((Value) expr.getLeft(), task);
		Number right = asNumber((Value) expr.getRight(), task);

		switch (expr.getOperator()) {
		case GREATER:
			return Numbers.greater(left, right);
		case GREATER_EQUAL:
			return Numbers.greaterEqual(left, right);
		case LESS:
			return Numbers.less(left, right);
		case LESS_EQUAL:
			return Numbers.lessEqual(left, right);
		}
		return false;
	}
	
	
	protected boolean _select(Words words, Content content){
		for (String word : words.getValues()) {
			if(!matches(content, word)){
				return false;
			}
		}
		return true;
	}
	
	protected boolean _select(Word word, Content content){
		return matches(content, word.getValue());
	}

	
	protected boolean _select(Phrase phrase, Content content){
		return matches(content, phrase.getValue());
	}
	
	protected boolean matches(Content content, String string) {
		return content.getValue().contains(string);
	}

	private Number asNumber(Value value, Task task) {
		String string = valueOf(value, task);
		return Numbers.parse(string);
	}

	private String valueOf(Value value, Task task) {
		if (value instanceof TagReference) {
			for (Tag tag : task.getTags()) {
				if (tag.getName().equals(value.getValue())) {
					return tag.getValue();
				}
			}
			return "";
		} else {
			return value.getValue();
		}
	}

	protected boolean isNotValue(Expression expr) {
		return expr == null || !(expr instanceof Value) || ((Value)expr).getValue() == null;
	}

	protected boolean _select(UnaryExpr expr, EObject object) {
		return !internalSelect(expr.getExpr(), object);
	}

	protected boolean _select(TagReference tagReference, Task task) {
		String tag = tagReference.getValue();
		for (Tag current : task.getTags()) {
			if (current.getName().startsWith(tag)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean _select(TaskReference taskReference, Task task) {
		return task.getValue().contains(taskReference.getValue());
	}
	
	protected boolean _select(TextReference taskReference, Note note) {
		return note.getValue().contains(taskReference.getValue());
	}

	protected boolean _select(ProjectReference projectReference, Project project) {
		String projectName = projectReference.getValue();
		if (projectName == null) {
			return true;
		}
		if(project.getValue() == null){
			return false;
		}
		boolean isProject = project.getValue().startsWith(projectName);
		if (isProject) {
			return true;
		}
		Container parentProject = project.getParent();
		if (parentProject instanceof Project) {
			return internalSelect(projectReference, parentProject);
		}
		return false;
	}

	protected boolean _select(ProjectReference projectReference, Content content) {
		Container parent = content.getParent();
		while (parent != null) {
			if (internalSelect(projectReference, parent)) {
				return true;
			}
			if (parent instanceof Project) {
				parent = ((Project) parent).getParent();
			} else {
				parent = null;
			}
		}
		return false;
	}

}