import React, { useState, useEffect } from 'react';

interface Patient {
    id: number;
    firstName: string;
    lastName: string;
    dateOfBirth: string;
}

const API_URL = 'https://localhost:8080/api/patients';

export default function ReceptionUI() {
    const [patients, setPatients] = useState<Patient[]>([]);
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [dateOfBirth, setDateOfBirth] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadPatients();
    }, []);

    const loadPatients = async () => {
        try {
            const response = await fetch(API_URL);
            const data = await response.json();
            setPatients(data);
        } catch (error) {
            console.error('Ошибка загрузки пациентов:', error);
        }
    };

    const handleAddPatient = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ firstName, lastName, dateOfBirth })
            });

            if (response.ok) {
                setFirstName('');
                setLastName('');
                setDateOfBirth('');
                await loadPatients();
                alert('Пациент успешно добавлен');
            } else {
                alert('Ошибка при добавлении пациента');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка соединения с сервером');
        } finally {
            setLoading(false);
        }
    };

    const handleDeletePatient = async (id: number) => {
        if (!confirm('Вы уверены, что хотите удалить пациента?')) {
            return;
        }

        try {
            const response = await fetch(`${API_URL}/${id}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                await loadPatients();
                alert('Пациент удалён');
            } else {
                alert('Ошибка при удалении пациента');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка соединения с сервером');
        }
    };

    return (
        <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
            <h1>Регистратура</h1>

            <div style={{
                border: '1px solid #ccc',
                padding: '20px',
                marginBottom: '20px',
                borderRadius: '8px'
            }}>
                <h2>Форма добавления пациента</h2>
                <form onSubmit={handleAddPatient}>
                    <div style={{ marginBottom: '10px' }}>
                        <label>
                            Имя:
                            <input
                                type="text"
                                value={firstName}
                                onChange={(e) => setFirstName(e.target.value)}
                                required
                                style={{
                                    marginLeft: '10px',
                                    padding: '5px',
                                    width: '200px'
                                }}
                            />
                        </label>
                    </div>

                    <div style={{ marginBottom: '10px' }}>
                        <label>
                            Фамилия:
                            <input
                                type="text"
                                value={lastName}
                                onChange={(e) => setLastName(e.target.value)}
                                required
                                style={{
                                    marginLeft: '10px',
                                    padding: '5px',
                                    width: '200px'
                                }}
                            />
                        </label>
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label>
                            Дата рождения:
                            <input
                                type="date"
                                value={dateOfBirth}
                                onChange={(e) => setDateOfBirth(e.target.value)}
                                required
                                style={{
                                    marginLeft: '10px',
                                    padding: '5px',
                                    width: '200px'
                                }}
                            />
                        </label>
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#4CAF50',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                        }}
                    >
                        {loading ? 'Отправка...' : 'Добавить пациента'}
                    </button>
                </form>
            </div>

            <div style={{
                border: '1px solid #ccc',
                padding: '20px',
                borderRadius: '8px'
            }}>
                <h2>Форма удаления пациента</h2>
                <p>Текущие пациенты: {patients.length}</p>

                {patients.length === 0 ? (
                    <p>Нет зарегистрированных пациентов</p>
                ) : (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr style={{ backgroundColor: '#f2f2f2' }}>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>ID</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Имя</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Фамилия</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Дата рождения</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Действия</th>
                        </tr>
                        </thead>
                        <tbody>
                        {patients.map(patient => (
                            <tr key={patient.id}>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                    {patient.id}
                                </td>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                    {patient.firstName}
                                </td>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                    {patient.lastName}
                                </td>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                                    {patient.dateOfBirth}
                                </td>
                                <td style={{ border: '1px solid #ddd', padding: '8px', textAlign: 'center' }}>
                                    <button
                                        onClick={() => handleDeletePatient(patient.id)}
                                        style={{
                                            padding: '5px 15px',
                                            backgroundColor: '#f44336',
                                            color: 'white',
                                            border: 'none',
                                            borderRadius: '4px',
                                            cursor: 'pointer'
                                        }}
                                    >
                                        Удалить
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}