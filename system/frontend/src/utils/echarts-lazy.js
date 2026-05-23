/**
 * ECharts 按需加载工具
 * 职责：动态导入 echarts/core 并注册所需组件，减小 vendor-echarts chunk 体积（~1MB → ~350KB）
 *
 * 使用方式：import { loadEcharts } from '../utils/echarts-lazy'
 * 替代：const echartsPromise = import('echarts')  ← 旧方式（全量导入）
 *
 * 包含组件：
 *   图表: PieChart, BarChart, LineChart
 *   坐标系: GridComponent
 *   交互: TooltipComponent, LegendComponent, TitleComponent
 *   渲染: CanvasRenderer
 */
let echartsPromise = null                                  // ECharts模块Promise缓存

export function loadEcharts() {
  if (!echartsPromise) {                                   // 首次调用才执行加载
    echartsPromise = import('echarts/core').then(async (echarts) => { // 动态导入核心
      // 合并 echarts/components 为单次导入（原先两次导入同一模块浪费带宽）
      const [
        { PieChart, BarChart, LineChart },                 // 图表类型组件
        { GridComponent, TooltipComponent, LegendComponent, TitleComponent }, // 交互组件
        { CanvasRenderer }                                 // Canvas渲染器
      ] = await Promise.all([                              // 并行加载子模块
        import('echarts/charts'),                          // 图表模块
        import('echarts/components'),                      // 交互组件模块
        import('echarts/renderers')                        // 渲染器模块
      ])
      echarts.use([                                        // 注册所有组件
        PieChart, BarChart, LineChart,
        GridComponent,
        TooltipComponent, LegendComponent, TitleComponent,
        CanvasRenderer
      ])
      return echarts                                       // 返回核心模块引用
    })
  }
  return echartsPromise                                    // 返回缓存的Promise
}