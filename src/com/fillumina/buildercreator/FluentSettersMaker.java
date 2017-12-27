package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class FluentSettersMaker {

    private final TreeMaker make;
    private final List<Tree> members;
    private final List<VariableElement> elements;
    private final String className;

    public FluentSettersMaker(TreeMaker make,
            List<Tree> members,
            List<VariableElement> elements,
            String className) {
        this.make = make;
        this.members = members;
        this.elements = elements;
        this.className = className;
    }

    int removeExistingFluentSetters(int index) {
        int counter = 0;
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.METHOD)) {
                MethodTree mt = (MethodTree) member;
                for (Element element : elements) {
                    if (mt.getName().contentEquals(element.getSimpleName())
                            && mt.getParameters().size() == 1
                            && mt.getReturnType() != null
                            && mt.getReturnType().getKind() == Tree.Kind.IDENTIFIER) {
                        treeIt.remove();
                        if (index > counter) {
                            index--;
                        }
                        break;
                    }
                }
            }
            counter++;
        }
        return index;
    }

    void addBuilderSetters(int index) {
        addFluentSetters(index, "with");
    }

    void addFluentSetters(int index) {
        addFluentSetters(index, null);
    }

    void addFluentSetters(int index, String preffix) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        int position = index - 1;
        for (VariableElement element : elements) {
            Name elementName = element.getSimpleName();
            VariableTree parameter = make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                    Collections.<AnnotationTree>emptyList()),
                    elementName,
                    make.Identifier(toStringWithoutPackages(element)),
                    null);

            ExpressionTree returnType = make.QualIdent(className);

            final String bodyText = createFluentSetterMethodBody(element);

            CharSequence methodName = preffix == null ? elementName : preffix + upFirstSymbol(String.valueOf(elementName));

            MethodTree method = make.Method(
                    make.Modifiers(modifiers, annotations),
                    methodName,
                    returnType,
                    Collections.<TypeParameterTree>emptyList(),
                    Collections.<VariableTree>singletonList(parameter),
                    Collections.<ExpressionTree>emptyList(),
                    bodyText,
                    null);

            position = Math.min(position + 1, members.size());
            members.add(position, method);
        }
    }

    private static String createFluentSetterMethodBody(Element element) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\nthis.")
                .append(element.getSimpleName())
                .append(" = ")
                .append(element.getSimpleName())
                .append(";\n")
                .append("return this;\n}");
        return sb.toString();
    }

    void addFields() {
        for (VariableElement element : elements) {
            VariableTree field
                    = make.Variable(make.Modifiers(
                            EnumSet.of(Modifier.PRIVATE),
                            Collections.<AnnotationTree>emptyList()),
                            element.getSimpleName().toString(),
                            make.Identifier(toStringWithoutPackages(element)),
                            null);

            members.add(field);
        }
    }

    private static String toStringWithoutPackages(VariableElement element) {
        return PackageHelper.removePackagesFromGenericsType(element.asType().toString());
    }

    public static String upFirstSymbol(String s) {
        char c[] = s.toCharArray();
        c[0] = Character.toUpperCase(c[0]);
        return new String(c);
    }
}
