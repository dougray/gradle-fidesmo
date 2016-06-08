package com.fidesmo.gradle.plugin;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.fidesmo.sec.models.Translations;
import com.fidesmo.sec.models.ServiceDescription;

public class DeleteRecipe {
    private final ServiceDescription description;
    private final List<DeleteAction> actions;
    private final Translations successMessage;
    private final Translations failureMessage;

    public static class DeleteContent {
        private final String application;
        private final Boolean withRelated = true;

        public DeleteContent(String application) {
            this.application = application;
        }
    }

    public static class DeleteAction {
        private final String endpoint = "/ccm/delete";
        private final DeleteContent content;

        public DeleteAction(String application) {
            this.content = new DeleteContent(application);
        }
    }

    public DeleteRecipe(ServiceDescription description, String instanceAid) {
        this.description = description;
        this.actions = new ArrayList<DeleteAction>();
        this.actions.add(new DeleteAction(instanceAid));
        Map<String, String> success = new HashMap();
        success.put("en", "Delete succeeded.");
        Map<String, String> failure = new HashMap();
        failure.put("en", "Delete failed.");
        this.successMessage = new Translations(success);
        this.failureMessage = new Translations(failure);
    }

}
