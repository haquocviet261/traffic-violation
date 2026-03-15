import React, { useState } from 'react';
import { Table, Image, Modal, Button } from 'antd';

interface Violation {
  id: string;
  frameNumber: number;
  timestamp: string;
  imagePath: string;
  eventType: string;
}

interface ViolationListProps {
  violations: Violation[];
}

const ViolationList: React.FC<ViolationListProps> = ({ violations }) => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);

  const redLightViolations = violations.filter(v => v.eventType === 'RED_LIGHT_VIOLATION');

  const columns = [
    {
      title: 'Frame',
      dataIndex: 'frameNumber',
      key: 'frameNumber',
    },
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (text: string) => new Date(text).toLocaleTimeString(),
    },
    {
      title: 'Vehicle Capture',
      dataIndex: 'imagePath',
      key: 'imagePath',
      render: (path: string) => (
        <Image
          width={100}
          src={`http://localhost:8080${path}`}
          placeholder={true}
          fallback="/placeholder-vehicle.png"
          style={{ cursor: 'pointer', borderRadius: '4px' }}
        />
      ),
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: Violation) => (
        <Button 
          type="primary" 
          onClick={() => {
            setSelectedImage(`http://localhost:8080${record.imagePath}`);
            setIsModalVisible(true);
          }}
        >
          View Full Capture
        </Button>
      ),
    },
  ];

  return (
    <div className="violation-list">
      <h3>Detected Red Light Violations ({redLightViolations.length})</h3>
      <Table 
        dataSource={redLightViolations} 
        columns={columns} 
        rowKey="id"
        pagination={{ pageSize: 5 }}
      />
      
      <Modal
        title="Violation Detail"
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={800}
      >
        {selectedImage && (
          <img src={selectedImage} alt="Violation" style={{ width: '100%', borderRadius: '8px' }} />
        )}
      </Modal>
    </div>
  );
};

export default ViolationList;
