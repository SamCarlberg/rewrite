/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.search;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;

public class UsesType<P> extends JavaIsoVisitor<P> {
    private final Pattern typePattern;

    public UsesType(String fullyQualifiedType) {
        this.typePattern = Pattern.compile(StringUtils.aspectjNameToPattern(fullyQualifiedType));
    }

    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, P p) {
        JavaSourceFile c = cu;

        for (JavaType.Method method : c.getTypesInUse().getUsedMethods()) {
            if (method.hasFlags(Flag.Static)) {
                if ((c = maybeMark(c, method.getDeclaringType())) != cu) {
                    return c;
                }
            }
        }

        for (JavaType type : c.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if ((c = maybeMark(c, fq)) != cu) {
                return c;
            }
        }

        for (J.Import anImport : c.getImports()) {
            if (anImport.isStatic()) {
                if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType()))) != cu) {
                    return c;
                }
            } else if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getType()))) != cu) {
                return c;
            }
        }

        return c;
    }

    private JavaSourceFile maybeMark(JavaSourceFile c, @Nullable JavaType.FullyQualified fq) {
        if (fq == null) {
            return c;
        }

        if (TypeUtils.isAssignableTo(typePattern, fq)) {
            return SearchResult.found(c);
        }

        return c;
    }
}
