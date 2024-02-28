/*
 Navicat Premium Data Transfer

 Source Server         : webos
 Source Server Type    : SQLite
 Source Server Version : 3030001
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3030001
 File Encoding         : 65001

 Date: 13/09/2022 16:30:54
*/

PRAGMA
foreign_keys = false;

-- ----------------------------
-- Table structure for io_drive
-- ----------------------------
DROP TABLE IF EXISTS "io_drive";
CREATE TABLE "io_drive"
(
    "id"                  text(32) NOT NULL,
    "name"                text(50),
    "path"                text(512),
    "use_size"            integer(20),
    "max_size"            integer(20),
    "avail_size"          integer(20),
    "token_id"            text(32),
    "user_drive_name"     text(512),
    "created_time"        text,
    "updated_time"        text,
    "drive_type"          text(20),
    "parent_user_id"      text(32),
    "no"                  integer(2),
    "real_file_path"      text(512),
    "second_transmission" integer(2),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_file_ass
-- ----------------------------
DROP TABLE IF EXISTS "io_file_ass";
CREATE TABLE "io_file_ass"
(
    "id"           text(32) NOT NULL,
    "user_id"      text(32),
    "ext"          text,
    "action"       text(30),
    "action_name"  text(30),
    "bind_key"     text(10),
    "sort_num"     integer(11),
    "soft_user_id" text(40),
    "url"          text(255),
    "icon_url"     text(255),
    "app_name"     text(100),
    "exp_action"   text(512),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_offline_download
-- ----------------------------
DROP TABLE IF EXISTS "io_offline_download";
CREATE TABLE "io_offline_download"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "down_url"       text(1000),
    "status"         integer(4),
    "user_id"        text(32),
    "parent_path"    text(512),
    "name"           text(50),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_user_drive
-- ----------------------------
DROP TABLE IF EXISTS "io_user_drive";
CREATE TABLE "io_user_drive"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "user_id"        text(32),
    "drive_id"       text(32),
    "name"           text(100),
    "path"           text(512),
    "use_size"       integer(20),
    "max_size"       integer(20),
    "avail_size"     integer(20),
    "created_time"   text,
    "updated_time"   text,
    "no"             integer(2),
    "valid"          integer(1),
    "is_system"      integer(1),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_user_recycle
-- ----------------------------
DROP TABLE IF EXISTS "io_user_recycle";
CREATE TABLE "io_user_recycle"
(
    "id"           text(32) NOT NULL,
    "user_id"      text(32),
    "name"         text(100),
    "size"         integer(20),
    "type"         integer(1),
    "deleted_time" text,
    "remove_path"  text(512),
    "real_path"    text(512),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_user_star
-- ----------------------------
DROP TABLE IF EXISTS "io_user_star";
CREATE TABLE "io_user_star"
(
    "id"      text(32) NOT NULL,
    "name"    text(255),
    "iud_id"  text(255),
    "path"    text(255),
    "user_id" text(255),
    "type"    text(255),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for share_file
-- ----------------------------
DROP TABLE IF EXISTS "share_file";
CREATE TABLE "share_file"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "code"           text(20),
    "user_id"        text(32),
    "view_num"       text(32),
    "down_num"       text(32),
    "share_time"     text,
    "password"       text(20),
    "expire_time"    text,
    "path"           text(255),
    "no"             integer(2),
    "files"          text(255),
    "name"           text(100),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for soft_apps
-- ----------------------------
DROP TABLE IF EXISTS "soft_apps";
CREATE TABLE "soft_apps"
(
    "id"                text(32) NOT NULL,
    "app_code"          text(30),
    "name"              text(50),
    "cat_code"          text(50),
    "current_version"   text(10),
    "last_version"      text(10),
    "home_pages"        text(255),
    "author"            text(50),
    "last_updated_time" text,
    "official_soft"     integer(4),
    "official_status"   integer(4),
    "icon_url"          text(255),
    "type"              integer(4),
    "iframe_url"        text(255),
    "down_url"          text(1000),
    "update_log"        text,
    "support_sys"       text(50),
    "support_version"   text(50),
    "user_id"           text(32),
    "cloud_app_id"      text(32),
    "setting_page"      text(100),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for soft_user
-- ----------------------------
DROP TABLE IF EXISTS "soft_user";
CREATE TABLE "soft_user"
(
    "id"           text(32) NOT NULL,
    "img_path"     text(255),
    "name"         text(100),
    "code"         text(50),
    "descr"        text,
    "screen_shots" text,
    "version"      text(10),
    "author"       text(30),
    "effect"       text,
    "type"         integer(1),
    "iframe_url"   text(255),
    "user_id"      text(40),
    "store_id"     text(40),
    "is_store"     integer(1),
    "download_url" text,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for soft_user_data
-- ----------------------------
DROP TABLE IF EXISTS "soft_user_data";
CREATE TABLE "soft_user_data"
(
    "id"       text(32) NOT NULL,
    "user_id"  text(32),
    "app_code" text(32),
    "data"     text,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS "sys_config";
CREATE TABLE "sys_config"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "open_reg"       integer(1),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS "sys_dict";
CREATE TABLE "sys_dict"
(
    "id"    text(32) NOT NULL,
    "code"  text(20),
    "name"  text(30),
    "descr" text(100),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_dict_detail
-- ----------------------------
DROP TABLE IF EXISTS "sys_dict_detail";
CREATE TABLE "sys_dict_detail"
(
    "id"     text(32) NOT NULL,
    "val"    text(100),
    "name"   text(30),
    "expand" text,
    "code"   text(32),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS "sys_log";
CREATE TABLE "sys_log"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "user_id"        text(32),
    "action"         text(30),
    "descr"          text(255),
    "action_time"    text,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "sys_menu";
CREATE TABLE "sys_menu"
(
    "id"             text(32) NOT NULL,
    "name"           text(100),
    "type"           integer(4),
    "parent_user_id" text(32),
    "auth_key"       text(50),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "sys_role";
CREATE TABLE "sys_role"
(
    "id"             text(32) NOT NULL,
    "name"           text(100),
    "auth_key"       text(100),
    "created_time"   text,
    "updated_time"   text,
    "parent_user_id" text(32),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "sys_role_menu";
CREATE TABLE "sys_role_menu"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "role_id"        text(32),
    "menu_id"        text(32),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_theme_user
-- ----------------------------
DROP TABLE IF EXISTS "sys_theme_user";
CREATE TABLE "sys_theme_user"
(
    "id"             text(32) NOT NULL,
    "parent_user_id" text(32),
    "user_id"        text(32),
    "theme_id"       text(32),
    "created_time"   text,
    "updated_time"   text,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_themes
-- ----------------------------
DROP TABLE IF EXISTS "sys_themes";
CREATE TABLE "sys_themes"
(
    "id"               text(32) NOT NULL,
    "name"             text(50),
    "background_url"   integer(11),
    "type"             integer(4),
    "user_id"          text(32),
    "css_down_url"     text(255),
    "background_color" text(50),
    "support_sys"      text(50),
    "official_theme"   integer(4),
    "official_status"  integer(4),
    "update_log"       text,
    "home_pages"       text(255),
    "author"           text(50),
    "cloud_theme_id"   integer(11),
    "cat_code"         text(20),
    "theme_code"       text(20),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "sys_user";
CREATE TABLE "sys_user"
(
    "id"             text(32) NOT NULL,
    "username"       text(50),
    "password"       text(255),
    "sp_password"    text(255),
    "img_path"       text,
    "nick_name"      text(50),
    "created_time"   text,
    "updated_time"   text,
    "theme_id"       text(32),
    "user_type"      integer(4),
    "parent_user_no" integer(11),
    "valid"          integer(1),
    "is_admin"       integer(1),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "sys_user_role";
CREATE TABLE "sys_user_role"
(
    "id"             text(32) NOT NULL,
    "user_id"        text(32),
    "role_id"        text(32),
    "parent_user_id" text(32),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for soft_user_office
-- ----------------------------
DROP TABLE IF EXISTS "soft_user_office";
CREATE TABLE "soft_user_office"
(
    "id"          text(40) NOT NULL,
    "path"        text(255),
    "user_id"     text(40),
    "expire_time" text,
    "jin_shan_id" text(20),
    "group_id"    text(20),
    `parent_path` text(512),
    `name`        text(255),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for io_token_data
-- ----------------------------
DROP TABLE IF EXISTS "io_token_data";
CREATE TABLE "io_token_data"
(
    "id"          text(40) NOT NULL,
    "drive_type"  text(255),
    "token_data"  text,
    "expire_time" text,
    "exp_data"    text(512),
    "err_count"   integer(2),
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict`
VALUES ('5c0f56a9828a4ce49450f892ddb3d1f9', 'IO_DRIVE_TYPE', '硬盘类型', NULL);
INSERT INTO `sys_dict`
VALUES ('c9abd295c07b4af797339764593d6e8d', 'USER_TYPE', '用户类型', '用户的类型');
INSERT INTO `sys_dict`
VALUES ('ceb0354033d443e69295af64f58ae2cb', 'USER_VALID', '是否有效', '用户是否有效');
INSERT INTO `sys_dict`
VALUES ('da963eab55c94937bf55bab21242cfdf', 'IS_OR_NO', '是与否', NULL);

-- ----------------------------
-- Records of sys_dict_detail
-- ----------------------------
INSERT INTO `sys_dict_detail`
VALUES ('2d096c28cf0446e5bcc3fa5848b262ee', '2', '禁用', NULL, 'USER_VALID');
INSERT INTO `sys_dict_detail`
VALUES ('2ff2c34e64534d38828df1018322d015', '1', '启用', NULL, 'USER_VALID');
INSERT INTO `sys_dict_detail`
VALUES ('30102c5eb3714a8fa18a523d88d95c98', '1', '是', NULL, 'IS_OR_NO');
INSERT INTO `sys_dict_detail`
VALUES ('3ad06e3fb4794cd3bd1c73df943fccaf', '2', '否', NULL, 'IS_OR_NO');
INSERT INTO `sys_dict_detail`
VALUES ('5a539c8b4c3b4dbaa07abe7bad5f659d', '1', '主用户', NULL, 'USER_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('66d59a69853f4dc5abd5f7f38898418d', '2', '子用户', NULL, 'USER_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('7da1631da16e4b32a63a10beb992960f', 'pan123', '123云盘', NULL, 'IO_DRIVE_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('8f45bf3e85874a048f4b1ebfbcecb069', 'local', '本地硬盘', NULL, 'IO_DRIVE_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('f3590af84de546b890915f1afeeca11d', 'aliyundrive', '阿里云盘', NULL, 'IO_DRIVE_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('746c4ca70ca7b5ba1e6bce0c54fa9b8c', 'pan189', '天翼云盘', NULL, 'IO_DRIVE_TYPE');
INSERT INTO `sys_dict_detail`
VALUES ('cbd11c0c3d757053b4be0d39f5e58d05', 'kodbox', '可道云', NULL, 'IO_DRIVE_TYPE');

PRAGMA
foreign_keys = true;
