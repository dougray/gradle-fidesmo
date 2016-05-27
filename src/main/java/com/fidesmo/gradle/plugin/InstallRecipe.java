package com.fidesmo.gradle.plugin;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.fidesmo.sec.models.Translations;
import com.fidesmo.sec.models.ServiceDescription;

public class InstallRecipe {
    private final ServiceDescription description;
    private final List<InstallAction> actions;
    private final Translations successMessage;
    private final Translations failureMessage;

    public static class InstallContent {
        private final String executableLoadFile;
        private final String executableModule;
        private final String application;
        private final Boolean encryptLoad;

        public InstallContent(String executableLoadFile, String executableModule, String application, Boolean encryptLoad) {
            this.executableLoadFile = executableLoadFile;
            this.executableModule = executableModule;
            this.application = application;
            this.encryptLoad = encryptLoad;
        }
    }

    public static class InstallAction {
        private final String endpoint = "/ccm/install";
        private final InstallContent content;

        public InstallAction(InstallContent content) {
            this.content = content;
        }
    }

    public InstallRecipe(ServiceDescription description, InstallContent content) {
        this.description = description;
        this.actions = new ArrayList<InstallAction>();
        this.actions.add(new InstallAction(content));
        Map<String, String> success = new HashMap();
        success.put("en", "Install succeeded.");
        Map<String, String> failure = new HashMap();
        failure.put("en", "Install failed.");
        this.successMessage = new Translations(success);
        this.failureMessage = new Translations(failure);
    }

}
