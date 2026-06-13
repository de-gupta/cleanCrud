import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.*;

class PatternProbe {
    static String probe(CreationOperationResult result) {
        return switch (result) {
            case CreatedCreationOperationResult<IdentifiedModel<String, String>>(var model) -> model.id();
            case QuarantinedCreationOperationResult ignored -> "q";
            case RejectedCreationOperationResult ignored -> "r";
        };
    }
}
