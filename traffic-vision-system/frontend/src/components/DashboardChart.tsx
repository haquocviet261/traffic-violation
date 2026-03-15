import React from 'react';
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

interface DashboardData {
  totalVideosProcessed: number;
  totalViolationsDetected: number;
  averageTrafficDensity: number;
}

function DashboardChart({ dashboardData }: { dashboardData: DashboardData }) {
  if (!dashboardData) {
    return <p>No chart data available.</p>;
  }

  const data = {
    labels: ['Videos Processed', 'Total Violations', 'Avg. Traffic Density (%)'],
    datasets: [
      {
        label: 'Overall Statistics',
        data: [
          dashboardData.totalVideosProcessed,
          dashboardData.totalViolationsDetected,
          (dashboardData.averageTrafficDensity * 100).toFixed(1), // Convert to percentage
        ],
        backgroundColor: [
          'rgba(75, 192, 192, 0.6)',
          'rgba(255, 99, 132, 0.6)',
          'rgba(54, 162, 235, 0.6)',
        ],
        borderColor: [
          'rgba(75, 192, 192, 1)',
          'rgba(255, 99, 132, 1)',
          'rgba(54, 162, 235, 1)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Traffic System Overview',
      },
    },
    scales: {
      y: {
        beginAtZero: true,
      },
    },
  };

  return (
    <div style={{ maxWidth: '700px', margin: 'auto' }}>
      <Bar data={data} options={options} />
    </div>
  );
}

export default DashboardChart;
