package com.cargill.common.xml;

import java.util.Optional;

public interface ProcessingDirectiveAccessor {

    default Optional<String> getDirective(String directiveName) {
        return Optional.empty();
    }

}
