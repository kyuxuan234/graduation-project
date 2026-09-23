<template>
  <div class="analysis">
    <div class="toolbar">
      <el-radio-group v-model="days" size="small" @change="load">
        <el-radio-button :label="7">近7天</el-radio-button>
        <el-radio-button :label="30">近30天</el-radio-button>
        <el-radio-button :label="90">近90天</el-radio-button>
      </el-radio-group>
      <p class="page-note">首页近 14 天只统计发布次数。这里分开看成败、失败落在哪、日志错误类型，以及告警走势。</p>
    </div>
    <div class="charts">
      <section class="panel panel-wide">
        <header>
          <h3>部署成败 · 按天</h3>
          <span class="hint">柱为完成次数，线为其中的失败次数</span>
        </header>
        <div ref="byDay" class="plot"></div>
      </section>
      <section class="panel">
        <header>
          <h3>失败分布</h3>
          <el-radio-group v-model="pieBy" size="mini" @change="drawPie">
            <el-radio-button label="app">按应用</el-radio-button>
            <el-radio-button label="server">按服务器</el-radio-button>
          </el-radio-group>
        </header>
        <div ref="pie" class="plot"></div>
      </section>
      <section class="panel">
        <header>
          <h3>执行日志错误 TOP</h3>
        </header>
        <div ref="errorTop" class="plot"></div>
      </section>
      <section class="panel panel-wide">
        <header class="is-center">
          <h3>告警数量与7日移动平均</h3>
        </header>
        <div ref="alarm" class="plot plot-tall"></div>
      </section>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

const PALETTE = ['#3d7eff', '#2fbe8f', '#f5a623', '#f56c6c', '#7c6cf0', '#36cfc9', '#ff85c0', '#8c8c8c']

export default {
  name: 'Analysis',
  data() {
    return {
      days: 30,
      pieBy: 'app',
      byApp: [],
      byServer: [],
      charts: []
    }
  },
  created() {
    this.load()
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    this.charts.forEach(chart => chart.dispose())
  },
  methods: {
    load() {
      Promise.all([
        this.$request.get('/statistics/failure', { params: { days: this.days } }),
        this.$request.get('/statistics/errorTop', { params: { days: this.days } }),
        this.$request.get('/statistics/alarmTrend', { params: { days: this.days } })
      ]).then(([failure, errorTop, alarm]) => {
        const fail = failure.data || { byApp: [], byServer: [], byDay: [] }
        this.byApp = fail.byApp || []
        this.byServer = fail.byServer || []
        this.charts.forEach(chart => chart.dispose())
        this.charts = []
        this.$nextTick(() => {
          this.drawOutcome(fail.byDay || [])
          this.drawPie()
          this.drawErrors(errorTop.data || [])
          this.drawAlarm(alarm.data || [])
        })
      })
    },
    resizeCharts() {
      this.charts.forEach(chart => chart.resize())
    },
    axisStyle() {
      return {
        axisLine: { lineStyle: { color: '#dcdfe6' } },
        axisTick: { show: false },
        axisLabel: { color: '#606266', fontSize: 12 },
        splitLine: { lineStyle: { color: '#f0f2f5' } }
      }
    },
    emptyTitle() {
      return {
        text: '暂无数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#c0c4cc', fontSize: 14, fontWeight: 400 }
      }
    },
    drawOutcome(rows) {
      const el = this.$refs.byDay
      if (!el) return
      const chart = echarts.init(el)
      this.charts.push(chart)
      const total = rows.reduce((sum, row) => sum + Number(row.total || 0), 0)
      const failed = rows.reduce((sum, row) => sum + Number(row.count || 0), 0)
      if (total === 0 && failed === 0) {
        chart.setOption({ title: this.emptyTitle() })
        return
      }
      const axis = this.axisStyle()
      chart.setOption({
        color: ['#8eb6ff', '#f56c6c'],
        tooltip: { trigger: 'axis' },
        legend: { data: ['完成次数', '失败次数'], right: 8, top: 0, itemWidth: 12, itemHeight: 8 },
        grid: { left: 8, right: 12, top: 36, bottom: 4, containLabel: true },
        xAxis: {
          type: 'category',
          data: rows.map(row => String(row.date).slice(5)),
          axisLine: axis.axisLine,
          axisTick: axis.axisTick,
          axisLabel: { color: '#606266', fontSize: 11, hideOverlap: true }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: axis.axisLabel,
          splitLine: axis.splitLine
        },
        series: [
          {
            name: '完成次数',
            type: 'bar',
            barMaxWidth: 16,
            itemStyle: { color: '#8eb6ff', borderRadius: [3, 3, 0, 0] },
            data: rows.map(row => Number(row.total || 0))
          },
          {
            name: '失败次数',
            type: 'line',
            symbol: 'circle',
            symbolSize: rows.length > 40 ? 0 : 6,
            lineStyle: { width: 2, color: '#f56c6c' },
            itemStyle: { color: '#f56c6c' },
            data: rows.map(row => Number(row.count || 0))
          }
        ]
      })
    },
    drawPie() {
      const el = this.$refs.pie
      if (!el) return
      let chart = this.charts.find(item => item.getDom && item.getDom() === el)
      if (!chart) {
        chart = echarts.init(el)
        this.charts.push(chart)
      }
      const rows = (this.pieBy === 'server' ? this.byServer : this.byApp).filter(row => Number(row.count) > 0)
      const sum = rows.reduce((acc, row) => acc + Number(row.count || 0), 0)
      if (sum === 0) {
        chart.setOption({ title: this.emptyTitle(), series: [] }, true)
        return
      }
      chart.setOption({
        color: PALETTE,
        title: {
          text: String(sum),
          subtext: '次失败',
          left: '34%',
          top: '40%',
          textAlign: 'center',
          textStyle: { fontSize: 22, fontWeight: 600, color: '#303133' },
          subtextStyle: { fontSize: 12, color: '#909399' }
        },
        tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 次（{d}%）' },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 0,
          top: 'middle',
          textStyle: { fontSize: 12, color: '#606266' }
        },
        series: [{
          type: 'pie',
          radius: ['48%', '72%'],
          center: ['34%', '50%'],
          data: rows.map(row => ({ name: row.name, value: Number(row.count) })),
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { show: false }
        }]
      }, true)
    },
    drawErrors(rows) {
      const el = this.$refs.errorTop
      if (!el) return
      const chart = echarts.init(el)
      this.charts.push(chart)
      const list = rows.filter(row => Number(row.count) > 0)
      if (!list.length) {
        chart.setOption({ title: this.emptyTitle() })
        return
      }
      const axis = this.axisStyle()
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 8, right: 28, top: 8, bottom: 4, containLabel: true },
        xAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: axis.axisLabel,
          splitLine: axis.splitLine
        },
        yAxis: {
          type: 'category',
          inverse: true,
          data: list.map(row => row.keyword),
          axisLine: axis.axisLine,
          axisTick: { show: false },
          axisLabel: { color: '#303133', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          barMaxWidth: 14,
          data: list.map(row => Number(row.count)),
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: params => PALETTE[params.dataIndex % PALETTE.length]
          },
          label: { show: true, position: 'right', color: '#606266', fontSize: 12 }
        }]
      })
    },
    drawAlarm(rows) {
      const el = this.$refs.alarm
      if (!el) return
      const chart = echarts.init(el)
      this.charts.push(chart)
      const sum = rows.reduce((acc, row) => acc + Number(row.count || 0), 0)
      if (sum === 0) {
        chart.setOption({ title: this.emptyTitle() })
        return
      }
      const axis = this.axisStyle()
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['告警数', '7日平均'], left: 'center', top: 0, itemWidth: 14, itemHeight: 8 },
        grid: { left: 8, right: 12, top: 36, bottom: 4, containLabel: true },
        xAxis: {
          type: 'category',
          data: rows.map(row => String(row.date).slice(5)),
          axisLine: axis.axisLine,
          axisTick: axis.axisTick,
          axisLabel: { color: '#606266', fontSize: 11, hideOverlap: true }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: axis.axisLabel,
          splitLine: axis.splitLine
        },
        series: [
          {
            name: '告警数',
            type: 'line',
            symbol: 'circle',
            symbolSize: rows.length > 40 ? 0 : 5,
            lineStyle: { width: 2, color: '#3d7eff' },
            itemStyle: { color: '#3d7eff' },
            areaStyle: { color: 'rgba(61, 126, 255, 0.12)' },
            data: rows.map(row => Number(row.count || 0))
          },
          {
            name: '7日平均',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 2, color: '#f5a623' },
            itemStyle: { color: '#f5a623' },
            data: rows.map(row => Number(row.avg || 0))
          }
        ]
      })
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px 16px;
  margin-bottom: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e9eef5;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(31, 42, 68, 0.05);
}
.page-note {
  margin: 0;
  color: #86909c;
  font-size: 13px;
  line-height: 1.5;
}
.charts {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 14px;
}
.panel {
  background: #fff;
  border: 1px solid #e9eef5;
  border-radius: 12px;
  padding: 14px 16px 10px;
  box-shadow: 0 6px 18px rgba(31, 42, 68, 0.05);
}
.panel-wide {
  grid-column: 1 / -1;
}
.panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  gap: 12px;
  margin-bottom: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f3f8;
}
.panel header.is-center {
  justify-content: center;
}
.panel h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2a44;
}
.hint {
  color: #86909c;
  font-size: 12px;
}
.plot {
  height: 300px;
}
.plot-tall {
  height: 340px;
}
@media (max-width: 1100px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
