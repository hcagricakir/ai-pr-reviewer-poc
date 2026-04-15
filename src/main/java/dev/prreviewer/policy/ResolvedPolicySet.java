package dev.prreviewer.policy;

import java.util.List;
import java.util.Map;

public record ResolvedPolicySet(
        List<String> policyIds,
        List<PolicyDocument.PolicyItem> principles,
        List<PolicyDocument.PolicyItem> checks,
        Map<String, String> severityMapping,
        PolicyDocument.OutputContract outputContract,
        Map<String, List<String>> domainOverrides
) {
}
