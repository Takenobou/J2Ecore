package aam65.j2ecore;

import org.eclipse.emf.ecore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoreUtils {
    private final Map<EClass, List<ReferenceInfo>> classReferences = new HashMap<>();

    public static class ReferenceInfo {
        EClass source;
        EClass target;
        String referenceName;
        boolean containment;

        public ReferenceInfo(EClass source, EClass target, String referenceName, boolean containment) {
            this.source = source;
            this.target = target;
            this.referenceName = referenceName;
            this.containment = containment;
        }
    }

    public void addReferenceInfo(EClass source, EClass target, String referenceName, boolean containment) {
        ReferenceInfo referenceInfo = new ReferenceInfo(source, target, referenceName, containment);
        classReferences.computeIfAbsent(source, k -> new ArrayList<>()).add(referenceInfo);
    }

    public Map<EClass, List<ReferenceInfo>> getClassReferences() {
        return classReferences;
    }
}
