package com.pingan.banzu.service;

import com.pingan.banzu.system.security.SystemPermissionService;
import org.springframework.stereotype.Service;

@Service
public class TrainingPermissionPolicy {

  private static final String LEGACY_TRAINING_VIEW = "PINGAN_TRAINING_VIEW";
  private static final String LEGACY_TRAINING_STUDY_EXAM = "PINGAN_TRAINING_STUDY_EXAM";
  private static final String LEGACY_TRAINING_MANAGE = "PINGAN_TRAINING_MANAGE";

  private final SystemPermissionService permissionService;

  public TrainingPermissionPolicy(SystemPermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public void assertCanViewExamTasks() {
    permissionService.assertHasAnyPermission(
        "PINGAN_TRAINING_EXAM_TASKS_VIEW", LEGACY_TRAINING_VIEW, LEGACY_TRAINING_STUDY_EXAM);
  }

  public void assertCanCreateExamTasks() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_EXAM_TASKS_CREATE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanDeleteExamTasks() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_EXAM_TASKS_DELETE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanDownloadExamTasks() {
    permissionService.assertHasAnyPermission(
        "PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD", LEGACY_TRAINING_VIEW, LEGACY_TRAINING_MANAGE);
  }

  public void assertCanViewExamResults() {
    permissionService.assertHasAnyPermission(
        "PINGAN_TRAINING_EXAM_RESULTS_VIEW", LEGACY_TRAINING_VIEW, LEGACY_TRAINING_STUDY_EXAM);
  }

  public void assertCanCreateExamResults() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_EXAM_RESULTS_CREATE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanDeleteExamResults() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_EXAM_RESULTS_DELETE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanViewSafetyLearning() {
    permissionService.assertHasAnyPermission(
        "PINGAN_TRAINING_SAFETY_LEARNING_VIEW", LEGACY_TRAINING_VIEW, LEGACY_TRAINING_STUDY_EXAM);
  }

  public void assertCanCreateSafetyLearning() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_SAFETY_LEARNING_CREATE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanDeleteSafetyLearning() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_SAFETY_LEARNING_DELETE", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanVoidSafetyLearning() {
    permissionService.assertHasAnyPermission("PINGAN_TRAINING_SAFETY_LEARNING_VOID", LEGACY_TRAINING_MANAGE);
  }

  public void assertCanDownloadSafetyLearning() {
    permissionService.assertHasAnyPermission(
        "PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD", LEGACY_TRAINING_VIEW, LEGACY_TRAINING_MANAGE);
  }
}
