package org.beckenburg;

/** Give standalone checks a bounded process lifecycle even if a failed Swing check leaves a timer alive. */
public final class VerificationMain {
    public static void main(String[] args){
        try{Class.forName("org.beckenburg."+args[0]).getMethod("main",String[].class).invoke(null,(Object)java.util.Arrays.copyOfRange(args,1,args.length));}
        catch(Throwable failure){if(failure instanceof java.lang.reflect.InvocationTargetException wrapped)failure=wrapped.getCause();failure.printStackTrace();System.exit(1);}
        System.exit(0);
    }
}
