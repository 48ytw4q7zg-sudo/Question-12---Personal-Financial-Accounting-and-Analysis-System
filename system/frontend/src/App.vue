<!--
  根组件
  职责：仅渲染路由出口 <router-view />，所有页面由路由系统控制显示
  对应关系：router/index.js 定义路由规则 → 匹配到的页面组件在此渲染
-->
<template>
  <router-view />
</template>

<style>
/* 全局基础样式：清除默认边距，设置全高，指定中文字体栈 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
