package org.fusesource.restygwt.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import org.fusesource.restygwt.client.Dispatcher;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestServiceProxy;
import org.fusesource.restygwt.client.TextCallback;
import org.fusesource.restygwt.client.callback.CallbackAware;
import org.fusesource.restygwt.rebind.util.OnceFirstIterator;

import static org.fusesource.restygwt.rebind.DirectRestServiceInterfaceClassCreator.DIRECT_REST_SERVICE_SUFFIX;

public class DirectRestServiceClassCreator extends DirectRestBaseSourceCreator {
    public static final String DIRECT_REST_IMPL_SUFFIX = DIRECT_REST_SERVICE_SUFFIX + "Impl";
    public static final String VOID_QUALIFIED_NAME = "void";
    private static final String STRING_QUALIFIED_NAME = String.class.getName();
    private static final String TEXT_CALLBACK_QUALIFIED_NAME = TextCallback.class.getName();

    public DirectRestServiceClassCreator(TreeLogger logger, GeneratorContext context, JClassType source) {
        super(logger, context, source, DIRECT_REST_IMPL_SUFFIX);
    }

    @Override
    protected ClassSourceFileComposerFactory createComposerFactory() throws UnableToCompleteException {
        return createClassSourceComposerFactory(JavaSourceCategory.CLASS,
                null,
                new String[]{
                        source.getParameterizedQualifiedSourceName(),
                        CallbackAware.class.getCanonicalName(),
                        RestServiceProxy.class.getCanonicalName()
                }
        );
    }

    @Override
    protected void generate() throws UnableToCompleteException {
        createRestyServiceField();
        createDelegateRestServiceProxyMethods();
        createCallbackSupportMethodsAndField();
        createServiceMethods();
    }

    private void createRestyServiceField() {
        String sourceName = getName(source) + DIRECT_REST_SERVICE_SUFFIX;
        p("private " + sourceName + " service = com.google.gwt.core.client.GWT.create(" + sourceName + ".class);");
    }

    private void createDelegateRestServiceProxyMethods() {
        String resourceClass = Resource.class.getCanonicalName();
        String dispatcherClass = Dispatcher.class.getCanonicalName();
        String restServiceProxyClass = RestServiceProxy.class.getCanonicalName();

        p("public final void setResource(" + resourceClass + " resource) {").i(1)
            .p("((" + restServiceProxyClass + ")service).setResource(resource);").i(-1)
        .p("}");

        p("public final " + resourceClass + " getResource() {").i(1)
            .p("return ((" + restServiceProxyClass + ")service).getResource();").i(-1)
        .p("}");

        p("public final void setDispatcher(" + dispatcherClass + " resource) {").i(1)
            .p("((" + restServiceProxyClass + ")service).setDispatcher(resource);").i(-1)
        .p("}");

        p("public final " + dispatcherClass + " getDispatcher() {").i(1)
            .p("return ((" + restServiceProxyClass + ")service).getDispatcher();").i(-1)
        .p("}");
    }

    private void createCallbackSupportMethodsAndField() {
        createCallbackField();
        createCallbackSetter();
        createVerifyCallbackMethod();
    }

    private void createServiceMethods() {
        for (JMethod method : source.getInheritableMethods()) {
            generateMethod(method);
        }
    }

    private void generateMethod(JMethod method) {
        p( getMethodDeclaration(method) + " {").i(1);
            generateCallVerifyCallback(method);
            generateCallServiceMethod(method);
            generateReturnNull(method);
        i(-1).p("}");
    }

    private String getMethodDeclaration(JMethod method) {
        return method.getReadableDeclaration(false, true, false, false, true);
    }

    private void generateCallVerifyCallback(JMethod method) {
        p("verifyCallback(\"" + method.getName() + "\", \"" + method.getReturnType().getQualifiedSourceName() + "\");");
    }

    private void generateCallServiceMethod(JMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        OnceFirstIterator<String> comma = new OnceFirstIterator<String>("", ", ");

        stringBuilder.append("service.")
                .append(method.getName())
                .append("(");

        for (JParameter parameter : method.getParameters()) {
            stringBuilder.append(comma.next())
                    .append(parameter.getName());
        }

        stringBuilder.append(comma.next());
        if (STRING_QUALIFIED_NAME.equals(method.getReturnType().getQualifiedSourceName())) {
            stringBuilder.append('(').append(TEXT_CALLBACK_QUALIFIED_NAME).append(')');
        }
        stringBuilder.append("this.callback");

        stringBuilder.append(");");

        p(stringBuilder.toString());
    }

    private void generateReturnNull(JMethod method) {
        if (isVoidMethod(method)) {
        	// check void first since JPrimitiveType will consider void to be a primitive
        	return;
        } else if (method.getReturnType().isPrimitive() != null) {
        	JPrimitiveType primitiveType = method.getReturnType().isPrimitive();
        	p("return " + primitiveType.getUninitializedFieldExpression() + ";");
    	} else {
            p("return null;");
        }
    }

    private static boolean isVoidMethod(JMethod method) {
        return VOID_QUALIFIED_NAME.equals(method.getReturnType().getQualifiedBinaryName());
    }

    private void createCallbackField() {
        p("private " + MethodCallback.class.getCanonicalName() + " callback;");
    }
    private void createCallbackSetter() {
        p( "public void setCallback(" + MethodCallback.class.getCanonicalName() + " callback) {").i(1)
            .p("this.callback = callback;").i(-1)
        .p("}");
    }

    private void createVerifyCallbackMethod() {
        p( "public void verifyCallback(String methodName, String returnType) {").i(1)
           .p("if (this.callback == null) {").i(1)
               .p("throw new IllegalArgumentException(" +
                       "\"You need to call this service with REST.withCallback(new MethodCallback<..>(){..}).call(service).\" + " +
                       "methodName + " +
                       "\"(..) and not try to access the service directly\");").i(-1)
           .p("} else if (\"" + STRING_QUALIFIED_NAME + "\".equals(returnType) && !(this.callback instanceof org.fusesource.restygwt.client.TextCallback)) {").i(1)
               .p("throw new IllegalArgumentException(\"Methods that return a String must use a TextCallback\");").i(-1)
           .p("}").i(-1)
        .p("}");
    }

}
