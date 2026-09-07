CREATE TABLE sys_org_content_profile (
  id BIGSERIAL PRIMARY KEY,
  org_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  subtitle VARCHAR(180) NULL,
  description TEXT NULL,
  image_attachment_id BIGINT NULL,
  video_attachment_id BIGINT NULL,
  video_title VARCHAR(180) NULL,
  video_sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_org_content_profile_org_deleted ON sys_org_content_profile (org_id, deleted);
CREATE INDEX idx_sys_org_content_profile_status ON sys_org_content_profile (status, deleted);
CREATE INDEX idx_sys_org_content_profile_video_sort ON sys_org_content_profile (video_sort_order, id);

INSERT INTO sys_menu (
  id, parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
) VALUES
  (
    1008,
    1000,
    'SYSTEM_CONTENT_PROFILE',
    '内容配置',
    '/system/content-profiles',
    '/system-management/content-profiles/index.vue',
    'lucide:images',
    'SYSTEM_CONTENT_PROFILE_VIEW',
    8,
    1,
    'ACTIVE'
  );
