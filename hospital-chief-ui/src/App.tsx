import { useState, useEffect } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';

interface Patient {
    id: number;
    firstName: string;
    lastName: string;
    dateOfBirth: string;
    createdAt: string;
}

export default function HospitalChiefUI() {
    const [patients, setPatients] = useState<Patient[]>([]);
    const [connected, setConnected] = useState(false);
    const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

    // Функция для загрузки текущего списка пациентов
    const loadCurrentPatients = async () => {
        try {
            const response = await fetch('http://localhost:8081/api/patients');
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
                setLastUpdate(new Date());
                console.log('Загружен текущий список пациентов:', data);
            }
        } catch (error) {
            console.error('Ошибка загрузки пациентов:', error);
        }
    };

    useEffect(() => {
        // Загружаем текущий список пациентов при старте
        loadCurrentPatients();

        const stompClient = new Client({
            brokerURL: 'ws://localhost:8081/ws', // Прямой WebSocket URL

            debug: (str) => {
                console.log('STOMP:', str);
            },

            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        let subscription: StompSubscription | undefined;

        stompClient.onConnect = () => {
            console.log('WebSocket подключен');
            setConnected(true);

            // Загружаем актуальный список после подключения
            loadCurrentPatients();

            // Подписываемся на топик с пациентами
            subscription = stompClient.subscribe('/topic/patients', (message) => {
                try {
                    const updatedPatients = JSON.parse(message.body);
                    console.log('Получены обновления пациентов через WebSocket:', updatedPatients);
                    setPatients(updatedPatients);
                    setLastUpdate(new Date());
                } catch (e) {
                    console.error('Ошибка парсинга сообщения:', e);
                }
            });
        };

        stompClient.onStompError = (frame) => {
            console.error('STOMP error:', frame.headers['message']);
            console.error('Details:', frame.body);
            setConnected(false);
        };

        stompClient.onWebSocketClose = () => {
            console.log('WebSocket закрыт');
            setConnected(false);
        };

        stompClient.onWebSocketError = (event) => {
            console.error('WebSocket error:', event);
            setConnected(false);
        };

        // Активируем подключение
        stompClient.activate();

        // Cleanup при размонтировании
        return () => {
            if (subscription) {
                subscription.unsubscribe();
            }
            stompClient.deactivate();
        };
    }, []);

    const formatDateTime = (date: Date) => {
        return date.toLocaleString('ru-RU');
    };

    const calculateAge = (dateOfBirth: string) => {
        const today = new Date();
        const birthDate = new Date(dateOfBirth);
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }

        return age;
    };

    return (
        <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto', fontFamily: 'Arial, sans-serif' }}>
            <h1 style={{ color: '#333' }}>Интерфейс главврача</h1>

            <div style={{
                border: '2px solid #ddd',
                padding: '20px',
                borderRadius: '8px',
                backgroundColor: '#f9f9f9'
            }}>
                <h2 style={{ marginTop: 0 }}>Список пациентов</h2>
                <p style={{ fontSize: '20px', fontWeight: 'bold', color: '#4CAF50' }}>
                    👥 Всего пациентов: {patients.length}
                </p>

                {patients.length === 0 ? (
                    <div style={{
                        padding: '40px',
                        textAlign: 'center',
                        backgroundColor: 'white',
                        borderRadius: '8px',
                        border: '1px dashed #ccc'
                    }}>
                        <p style={{ fontSize: '18px', color: '#999', margin: 0 }}>
                            📋 Нет пациентов в системе
                        </p>
                        <p style={{ fontSize: '14px', color: '#bbb', marginTop: '10px' }}>
                            Добавьте пациента через интерфейс регистратуры
                        </p>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{
                            width: '100%',
                            borderCollapse: 'collapse',
                            backgroundColor: 'white',
                            marginTop: '15px',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                        }}>
                            <thead>
                            <tr style={{ backgroundColor: '#4CAF50', color: 'white' }}>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'left' }}>ID</th>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'left' }}>Имя</th>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'left' }}>Фамилия</th>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>Дата рождения</th>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>Возраст</th>
                                <th style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>Дата регистрации</th>
                            </tr>
                            </thead>
                            <tbody>
                            {patients.map((patient, index) => (
                                <tr
                                    key={patient.id}
                                    style={{
                                        backgroundColor: index % 2 === 0 ? '#f9f9f9' : 'white',
                                        transition: 'background-color 0.2s'
                                    }}
                                >
                                    <td style={{ border: '1px solid #ddd', padding: '12px' }}>
                                        <strong>#{patient.id}</strong>
                                    </td>
                                    <td style={{ border: '1px solid #ddd', padding: '12px' }}>
                                        {patient.firstName}
                                    </td>
                                    <td style={{ border: '1px solid #ddd', padding: '12px' }}>
                                        {patient.lastName}
                                    </td>
                                    <td style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>
                                        {patient.dateOfBirth}
                                    </td>
                                    <td style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>
                      <span style={{
                          padding: '4px 8px',
                          backgroundColor: '#e3f2fd',
                          borderRadius: '4px',
                          fontWeight: 'bold'
                      }}>
                        {calculateAge(patient.dateOfBirth)} лет
                      </span>
                                    </td>
                                    <td style={{ border: '1px solid #ddd', padding: '12px', textAlign: 'center' }}>
                                        {patient.createdAt}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}