import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.*;

class PatternProbe2<ID, M> {
    String probe(CreationOperationResult result) {
        return switch (result) {
            case CreatedCreationOperationResult(IdentifiedModel<ID, M> model) -> String.valueOf(model.id());
            case QuarantinedCreationOperationResult ignored -> "q";
            case RejectedCreationOperationResult ignored -> "r";
        };
    }
}
