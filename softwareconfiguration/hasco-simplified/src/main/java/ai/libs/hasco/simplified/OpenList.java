package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

public interface OpenList {

    boolean hasNext();

    ComponentInstance popNext();

}
