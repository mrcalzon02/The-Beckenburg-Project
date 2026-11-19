package org.beckenburg;

/** No legacy STEP ACK can stand in for actual 50Hz host-authorized movement. */
public final class AuthoritativeBodyMotionProbeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static String body(int seat,int x,int z,int tick){
        return "BODY|"+seat+"|"+x+"|0|"+z+"|180|"+tick;
    }
    public static void main(String[] args){
        var proof=new AuthoritativeBodyMotionProbe(0);
        proof.accept("ACCEPT|STEP",true);
        check(proof.samples()==0,"legacy ACK is not authoritative movement");
        proof.accept(body(0,1000,2000,1),false);
        proof.accept(body(0,1000,2000,1),true);
        proof.accept(body(1,9000,5000,1),true);
        check(proof.samples()==0,"unchanged body or other hunter does not count");
        proof.accept(body(0,1020,2000,1),true);
        proof.accept(body(0,1040,2000,1),true);
        check(proof.samples()==2&&Math.abs(proof.metres()-.04)<1e-8,
            "same-tick 50Hz BODY updates must prove real authorized travel");
        proof.accept(body(0,8000,2000,2),true);
        check(proof.samples()==2,"teleport cannot pass a walk benchmark");
        proof.accept(body(0,8040,2000,2),true);
        check(proof.samples()==3,"legal movement after rebase counts");
        proof.accept(body(0,8080,2000,3),false);
        check(proof.samples()==3,"idle movement does not contaminate walking segment");
        System.out.println("PASS: host BODY motion proof, 50Hz same-tick acceptance, teleport and idle exclusion");
    }
}
