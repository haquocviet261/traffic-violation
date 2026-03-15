import React from 'react';
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Card, Col, Row, Statistic } from 'antd'; // Import Ant Design components

ChartJS.register(ArcElement, Tooltip, Legend);

interface TrafficStatsProps {
  stats: {
    vehicleCount: number;
    violationCount: number;
    trafficDensity: number;
    trafficLightDetections: number;
    analysisTime: string;
  } | null;
}

const TrafficStats: React.FC<TrafficStatsProps> = ({ stats }) => {
  if (!stats) {
    return <div className="traffic-stats-container">No statistics available.</div>;
  }

  const chartData = {
    labels: ['Total Vehicle Detections', 'Traffic Lights Detected', 'Red Light Violations'],
    datasets: [
      {
        data: [stats.vehicleCount, stats.trafficLightDetections, stats.violationCount],
        backgroundColor: [
          'rgba(75, 192, 192, 0.6)', // Greenish for vehicles
          'rgba(54, 162, 235, 0.6)', // Bluish for traffic lights
          'rgba(255, 99, 132, 0.6)', // Reddish for violations
        ],
        borderColor: [
          'rgba(75, 192, 192, 1)',
          'rgba(54, 162, 235, 1)',
          'rgba(255, 99, 132, 1)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Analysis Overview',
      },
    },
  };

  return (
    <div className="traffic-stats-container">
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="Total Vehicle Detections" value={stats.vehicleCount} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Traffic Lights Detected" value={stats.trafficLightDetections} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Red Light Violations" value={stats.violationCount} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Traffic Density" value={(stats.trafficDensity * 100).toFixed(2)} suffix="%" />
          </Card>
        </Col>
      </Row>
      <Row gutter={16} style={{ marginTop: '1rem' }}>
        <Col span={24}>
          <Card>
            <Statistic title="Analysis Time" value={new Date(stats.analysisTime).toLocaleString()} />
          </Card>
        </Col>
      </Row>

      <div style={{ height: '300px', marginTop: '1.5rem' }}>
        <Doughnut data={chartData} options={chartOptions} />
      </div>
    </div>
  );
};

export default TrafficStats;
