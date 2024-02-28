/*
 Navicat Premium Data Transfer

 Source Server         : webos_tenfell_cn
 Source Server Type    : MySQL
 Source Server Version : 50737
 Source Host           : 43.138.169.186:3306
 Source Schema         : webos_tenfell_cn

 Target Server Type    : MySQL
 Target Server Version : 50737
 File Encoding         : 65001

 Date: 13/09/2022 16:30:32
*/

SET NAMES utf8mb4;
SET
    FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for io_drive
-- ----------------------------
DROP TABLE IF EXISTS `io_drive`;
CREATE TABLE `io_drive`
(
    `id`                  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `name`                varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '硬盘名称',
    `path`                varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '物理路径,此路径之后的位置为用户可操作的位置,例如:/test',
    `use_size`            int(20)                                                 NULL DEFAULT NULL COMMENT '已用大小MB,等价于所有user_drive的max_size和',
    `max_size`            int(20)                                                 NULL DEFAULT NULL COMMENT '最大大小MB,0表示无限制',
    `avail_size`          int(20)                                                 NULL DEFAULT NULL COMMENT '可用大小MB,0表示无限制',
    `token_id`            varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL COMMENT 'token表数据',
    `user_drive_name`     varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'path对应的中文名,主要针对第三方网盘,防止不知道真实路径为多少,此字段为系统备用字段,用户无感知',
    `created_time`        datetime                                                NULL DEFAULT NULL,
    `updated_time`        datetime                                                NULL DEFAULT NULL,
    `drive_type`          varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '硬盘类型',
    `parent_user_id`      varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    `no`                  int(2),
    `real_file_path`      varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '真实文件路径',
    `second_transmission` int(2),
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '硬盘表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for io_file_ass
-- ----------------------------
DROP TABLE IF EXISTS `io_file_ass`;
CREATE TABLE `io_file_ass`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `user_id`      varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户id',
    `ext`          text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL DEFAULT NULL COMMENT '拓展名',
    `action`       varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '行为addall,openwith,new',
    `action_name`  varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '操作名称',
    `bind_key`     varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '注册的按钮A-Z大写',
    `sort_num`     int(11)                                                 NULL DEFAULT NULL COMMENT '排序,越大越靠前',
    `soft_user_id` varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '软件关联id',
    `url`          varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地址',
    `icon_url`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图标地址',
    `app_name`     varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '软件名称',
    `exp_action`   varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '拓展动作',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '文件关联打开'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for io_offline_download
-- ----------------------------
DROP TABLE IF EXISTS `io_offline_download`;
CREATE TABLE `io_offline_download`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '主用户id',
    `down_url`       varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '下载地址',
    `status`         int(4)                                                   NULL DEFAULT NULL COMMENT '0已创建1下载中2下载完成3下载失败',
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '用户id',
    `parent_path`    varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '当前文件的位置相对于user_drive的位置,如{io:1}/abc',
    `name`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '文件名',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '离线下载'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for io_user_drive
-- ----------------------------
DROP TABLE IF EXISTS `io_user_drive`;
CREATE TABLE `io_user_drive`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户id',
    `drive_id`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '硬盘id',
    `name`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '磁盘名称',
    `path`           varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '相对于drive的位置,此位置之后为该用户可操作的位置,例如{1}/abc',
    `use_size`       int(20)                                                 NULL DEFAULT NULL COMMENT '已用大小MB',
    `max_size`       int(20)                                                 NULL DEFAULT NULL COMMENT '最大大小MB,不能为0',
    `avail_size`     int(20)                                                 NULL DEFAULT NULL COMMENT '可用大小MB,不可为0',
    `created_time`   datetime                                                NULL DEFAULT NULL,
    `updated_time`   datetime                                                NULL DEFAULT NULL,
    `no`             int(2)                                                  NULL DEFAULT NULL COMMENT '磁盘编号',
    `valid`          int(1)                                                  NULL DEFAULT NULL COMMENT '是否有效1有效 2无效',
    `is_system`      int(1)                                                  NULL DEFAULT NULL COMMENT '是否系统盘',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户磁盘表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for io_user_recycle
-- ----------------------------
DROP TABLE IF EXISTS `io_user_recycle`;
CREATE TABLE `io_user_recycle`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `user_id`      varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL,
    `name`         varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件名',
    `size`         int(20)                                                 NULL DEFAULT NULL COMMENT '大小',
    `type`         int(1)                                                  NULL DEFAULT NULL COMMENT '类型',
    `deleted_time` datetime                                                NULL DEFAULT NULL COMMENT '删除时间',
    `remove_path`  varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '原删除路径',
    `real_path`    varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备份路径(仅限本地文件)',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户回收站'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for io_user_star
-- ----------------------------
DROP TABLE IF EXISTS `io_user_star`;
CREATE TABLE `io_user_star`
(
    `id`      varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `name`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '显示名称',
    `iud_id`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户磁盘id',
    `path`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '路径以{io}或{uio开头}',
    `user_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
    `type`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for share_file
-- ----------------------------
DROP TABLE IF EXISTS `share_file`;
CREATE TABLE `share_file`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    `code`           varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分享编码',
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分享人',
    `view_num`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '浏览次数',
    `down_num`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '下载次数',
    `share_time`     datetime                                                NULL DEFAULT NULL COMMENT '分享时间',
    `password`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '密码',
    `expire_time`    datetime                                                NULL DEFAULT NULL COMMENT '到期时间',
    `path`           varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '以{io:0}或者{uio:0}开头',
    `no`             int(2)                                                  NULL DEFAULT NULL,
    `files`          varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多个分号分割',
    `name`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '分享的名称',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '分享管理'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for soft_apps
-- ----------------------------
DROP TABLE IF EXISTS `soft_apps`;
CREATE TABLE `soft_apps`
(
    `id`                varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NOT NULL,
    `app_code`          varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '软件编码/软件目录',
    `name`              varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '软件名称',
    `cat_code`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '分类编码',
    `current_version`   varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '当前版本号',
    `last_version`      varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '最新版本号',
    `home_pages`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '软件主页',
    `author`            varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '作者名称',
    `last_updated_time` datetime                                                 NULL DEFAULT NULL COMMENT '最后从官方拉取的时间',
    `official_soft`     int(4)                                                   NULL DEFAULT NULL COMMENT '0非官方软件1官方软件',
    `official_status`   int(4)                                                   NULL DEFAULT NULL COMMENT '软件状态0未提交1审核中2已上架3已打回',
    `icon_url`          varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '图标https地址',
    `type`              int(4)                                                   NULL DEFAULT NULL COMMENT '0插件1轻应用',
    `iframe_url`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT 'type=1才有效,轻应用地址',
    `down_url`          varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'type=0才有效,软件包下载地址',
    `update_log`        mediumtext CHARACTER SET utf8 COLLATE utf8_general_ci    NULL COMMENT '更新日志',
    `support_sys`       varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '支持系统macos,deepin,win11,win10,mobile,英文逗号分割',
    `support_version`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '支持的版本和support_sys是同步的,逗号分割,均支持请用0代替',
    `user_id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT 'official_soft为0时必填',
    `cloud_app_id`      varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci   NULL DEFAULT NULL COMMENT '远程商城的app_id',
    `setting_page`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '相对插件目录地址',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '本地+官方软件列表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for soft_user
-- ----------------------------
DROP TABLE IF EXISTS `soft_user`;
CREATE TABLE `soft_user`
(
    `id`           varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `img_path`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图标地址',
    `name`         varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
    `code`         varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '编码(轻应用编码为md5的iframeUrl)',
    `descr`        text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL COMMENT '描述',
    `screen_shots` text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL COMMENT '截图json',
    `version`      varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '版本号',
    `author`       varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '作者',
    `effect`       text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL DEFAULT NULL COMMENT '功能描述',
    `type`         int(1)                                                  NULL DEFAULT NULL COMMENT '0插件1轻应用',
    `iframe_url`   varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '轻应用地址',
    `user_id`      varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户id',
    `store_id`     varchar(40) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '商城id',
    `is_store`     int(1)                                                  NULL DEFAULT NULL COMMENT '1商城app 0本地app',
    `download_url` text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL DEFAULT NULL COMMENT '下载地址',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for soft_user_data
-- ----------------------------
DROP TABLE IF EXISTS `soft_user_data`;
CREATE TABLE `soft_user_data`
(
    `id`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `user_id`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
    `app_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '软件编号',
    `data`     text CHARACTER SET utf8 COLLATE utf8_general_ci        NULL COMMENT '用户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户软件数据表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `open_reg`       int(1)                                                 NULL DEFAULT NULL COMMENT '开放注册1开0关',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '系统配置'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`
(
    `id`    varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `code`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '字典编码',
    `name`  varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '字典中文名',
    `descr` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典中文描述',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '字典表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_detail
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_detail`;
CREATE TABLE `sys_dict_detail`
(
    `id`     varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `val`    varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典项的值',
    `name`   varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '字典项中文',
    `expand` mediumtext CHARACTER SET utf8 COLLATE utf8_general_ci   NULL COMMENT '拓展功能',
    `code`   varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '字典id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '字典详情表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户id',
    `action`         varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '操作内容',
    `descr`          varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
    `action_time`    datetime                                                NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '系统日志'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `name`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '功能名',
    `type`           int(4)                                                  NULL DEFAULT NULL COMMENT '类型0-9功能10行为0设置',
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    `auth_key`       varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '功能表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `name`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色名称',
    `auth_key`       varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限编码',
    `created_time`   datetime                                                NULL DEFAULT NULL,
    `updated_time`   datetime                                                NULL DEFAULT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主用户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '角色表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主用户id',
    `role_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `menu_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '角色菜单'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_theme_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_theme_user`;
CREATE TABLE `sys_theme_user`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主用户id',
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
    `theme_id`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商城主题id',
    `created_time`   datetime                                               NULL DEFAULT NULL,
    `updated_time`   datetime                                               NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户主题表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_themes
-- ----------------------------
DROP TABLE IF EXISTS `sys_themes`;
CREATE TABLE `sys_themes`
(
    `id`               varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL,
    `name`             varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL,
    `background_url`   int(11)                                                 NULL DEFAULT NULL,
    `type`             int(4)                                                  NULL DEFAULT NULL COMMENT '0网络主题1自定义主题',
    `user_id`          varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT 'type=1时必填',
    `css_down_url`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '样式表下载地址',
    `background_color` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '和background_url只能二选一',
    `support_sys`      varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT 'macos,deepin,win11,win10,mobile单选',
    `official_theme`   int(4)                                                  NULL DEFAULT NULL COMMENT '0非官方 1官方',
    `official_status`  int(4)                                                  NULL DEFAULT NULL COMMENT '软件状态0未提交1审核中2已上架3已打回',
    `update_log`       mediumtext CHARACTER SET utf8 COLLATE utf8_general_ci   NULL COMMENT '更新日志',
    `home_pages`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主题主页',
    `author`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '作者',
    `cloud_theme_id`   int(11)                                                 NULL DEFAULT NULL COMMENT '远程商城主题id',
    `cat_code`         varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '分类编码',
    `theme_code`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '主题编码/目录',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '本地+商城主题表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '主键id',
    `username`       varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '用户名',
    `password`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
    `sp_password`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '锁屏密码',
    `img_path`       text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL DEFAULT NULL COMMENT '文件路径',
    `nick_name`      varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '昵称',
    `created_time`   datetime                                                NULL DEFAULT NULL COMMENT '创建时间',
    `updated_time`   datetime                                                NULL DEFAULT NULL COMMENT '更新时间',
    `theme_id`       varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci  NULL DEFAULT NULL COMMENT '当前主题',
    `user_type`      int(4)                                                  NULL DEFAULT NULL COMMENT '用户类型1主用户2子用户',
    `parent_user_no` int(11)                                                 NULL DEFAULT NULL COMMENT '父级用户编号',
    `valid`          int(1)                                                  NULL DEFAULT NULL COMMENT '是否有效1有效,2无效',
    `is_admin`       int(1)                                                  NULL DEFAULT NULL COMMENT '是否管理员',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `id`             varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `user_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主用户或子用户id',
    `role_id`        varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `parent_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主用户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '用户角色关联表'
  ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for soft_user_office
-- ----------------------------
DROP TABLE IF EXISTS `soft_user_office`;
CREATE TABLE `soft_user_office`
(
    `id`          varchar(40) NOT NULL,
    `path`        varchar(255) DEFAULT NULL,
    `user_id`     varchar(40)  DEFAULT NULL,
    `expire_time` datetime     DEFAULT NULL,
    `jin_shan_id` varchar(20)  DEFAULT NULL,
    `group_id`    varchar(20)  DEFAULT NULL,
    `parent_path` varchar(512) DEFAULT NULL,
    `name`        varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- ----------------------------
-- Table structure for io_token_data
-- ----------------------------
DROP TABLE IF EXISTS `io_token_data`;
CREATE TABLE `io_token_data`
(
    `id`          varchar(40) NOT NULL,
    `drive_type`  varchar(255) DEFAULT NULL,
    `token_data`  text         DEFAULT NULL,
    `expire_time` datetime     DEFAULT NULL,
    `exp_data`    varchar(512) DEFAULT NULL,
    `err_count`   int(2)       DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

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

SET
    FOREIGN_KEY_CHECKS = 1;
