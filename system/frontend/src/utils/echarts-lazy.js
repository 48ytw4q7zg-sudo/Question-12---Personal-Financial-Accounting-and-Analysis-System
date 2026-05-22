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
let echartsPromise = null

export function loadEcharts() {
  if (!echartsPromise) {
    echartsPromise = import('echarts/core').then(async (echarts) => {
      // 合并 echarts/components 为单次导入（原先两次导入同一模块浪费带宽）
      const [
        { PieChart, BarChart, LineChart },
        { GridComponent, TooltipComponent, LegendComponent, TitleComponent },
        { CanvasRenderer }
      ] = await Promise.all([
        import('echarts/charts'),
        import('echarts/components'),
        import('echarts/renderers')
      ])
      echarts.use([
        PieChart, BarChart, LineChart,
        GridComponent,
        TooltipComponent, LegendComponent, TitleComponent,
        CanvasRenderer
      ])
      return echarts
    })
  }
  return echartsPromise
}