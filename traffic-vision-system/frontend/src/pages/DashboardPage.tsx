import React, { useEffect, useState } from 'react';
import { getDashboardStats } from '../services/api';
import DashboardChart from '../components/DashboardChart'; // Import the new component

interface DashboardData {
  totalVideosProcessed: number;
  totalViolationsDetected: number;
  averageTrafficDensity: number;
  lastUpdated: string;
}

function DashboardPage() {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        setLoading(true);
        const data = await getDashboardStats();
        setDashboardData(data);
      } catch (err: unknown) {
        if (err instanceof Error) {
          setError('Failed to load dashboard data: ' + err.message);
        } else {
          setError('Failed to load dashboard data: An unknown error occurred.');
        }
      } finally {
        setLoading(false);
      }
    };
    loadDashboardData();
  }, []);

  if (loading) return <div className="dashboard-page">Loading Dashboard...</div>;
  if (error) return <div className="dashboard-page">Error: {error}</div>;

  return (
    <div className="dashboard-page">
      <h2>Traffic Analysis Dashboard</h2>
      {dashboardData && (
        <div className="dashboard-summary">
          <div className="card">
            <h3>Videos Processed</h3>
            <p>{dashboardData.totalVideosProcessed}</p>
          </div>
          <div className="card">
            <h3>Total Violations</h3>
            <p>{dashboardData.totalViolationsDetected}</p>
          </div>
          <div className="card">
            <h3>Avg. Traffic Density</h3>
            <p>{(dashboardData.averageTrafficDensity * 100).toFixed(1)}%</p>
          </div>
          <div className="card">
            <h3>Last Updated</h3>
            <p>{new Date(dashboardData.lastUpdated).toLocaleString()}</p>
          </div>
        </div>
      )}

      <div className="charts-section">
        <h3>Traffic Trends</h3>
        <div className="chart-container">
          {dashboardData ? (
            <DashboardChart dashboardData={dashboardData} />
          ) : (
            <p>Loading chart data...</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;
