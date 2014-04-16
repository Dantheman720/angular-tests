/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.angularjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.jpa.ui.NewEntityCommand;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.javaee.rest.ui.setup.RestSetupWizard;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.scaffold.impl.ui.ScaffoldExecuteGenerationStep;
import org.jboss.forge.addon.scaffold.impl.ui.ScaffoldGenerateCommandImpl;
import org.jboss.forge.addon.scaffold.ui.ScaffoldGenerateCommand;
import org.jboss.forge.addon.scaffold.ui.ScaffoldSetupWizard;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests to verify that Freemarker templates that generate JavaScript work. Verifies that the templates dont error out
 * during processing. Functional tests verify whether the generated JavaScript actually work.
 */
@RunWith(Arquillian.class)
public class AngularScaffoldTest
{

   @Inject
   private ProjectFactory projectFactory;

   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
            @AddonDependency(name = "org.jboss.forge.addon:angularjs"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:maven"),
            @AddonDependency(name = "org.jboss.forge.addon:javaee"),
            @AddonDependency(name = "org.jboss.forge.addon:scaffold")
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML()
               .addAsAddonDependencies(AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"))
               .addAsAddonDependencies(AddonDependencyEntry.create("org.jboss.forge.addon:javaee"))
               .addAsAddonDependencies(AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"))
               .addAsAddonDependencies(AddonDependencyEntry.create("org.jboss.forge.addon:scaffold"));
   }

   @Inject
   private UITestHarness harness;

   private Project project = null;

   @Test
   public void testGenerateAngularApp() throws Exception
   {
      Project project = getProject();

      FileResource<?> search = project.getRoot().getChild("src/main/webapp/views/Ingredient/search.html")
               .reify(FileResource.class);

      String contents = search.getContents();
      Assert.assertNotNull(contents);
   }

   private Project getProject() throws Exception
   {
      if (project == null)
      {
         List<Class<? extends ProjectFacet>> facets = new ArrayList<>();
         facets.add(JavaSourceFacet.class);
         project = projectFactory.createTempProject(facets);
         Assert.assertNotNull(project);

         CommandController scaffoldSetup = harness.createCommandController(ScaffoldSetupWizard.class);
         scaffoldSetup.initialize();
         scaffoldSetup.setValueFor("provider", "AngularJS");
         Assert.assertTrue(scaffoldSetup.canExecute());
         Result result = scaffoldSetup.execute();
         Assert.assertTrue(!(result instanceof Failed));

         WizardCommandController jpaSetup = harness.createWizardController(JPASetupWizard.class, project.getRoot());
         jpaSetup.initialize();
         Assert.assertTrue(jpaSetup.isEnabled());
         jpaSetup.next();
         Result resultJPA = jpaSetup.execute();

         CommandController restSetup = harness.createCommandController(RestSetupWizard.class, project.getRoot());
         restSetup.initialize();
         Result resultRest = restSetup.execute();

         CommandController entitySetup = harness.createCommandController(NewEntityCommand.class, project.getRoot());
         entitySetup.initialize();
         entitySetup.setValueFor("named", "Ingredient");
         entitySetup.setValueFor("targetPackage", "org.forge.angular.test");
         Result resultEntity = entitySetup.execute();

         WizardCommandController generator = harness.createWizardController(ScaffoldGenerateCommand.class,
                  project.getRoot());
         generator.initialize();
         generator.setValueFor("provider", "AngularJS");
         generator.next();
         generator.setValueFor("targets", Arrays.asList("org.forge.angular.test.Ingredient"));
         generator.next();
         generator.next();
         Result resultGen = generator.execute();
      }
      return project;
   }
}
