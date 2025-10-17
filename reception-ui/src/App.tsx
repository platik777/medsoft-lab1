import React, { useState, useEffect } from 'react';

interface Patient {
    id: number;
    firstName: string;
    lastName: string;
    dateOfBirth: string;
}

interface User {
    username: string;
    fullName: string;
    token: string;
}

const API_URL = 'https://localhost:8080';

export default function ReceptionUI() {
    const [user, setUser] = useState<User | null>(null);
    const [patients, setPatients] = useState<Patient[]>([]);

    const [loginMode, setLoginMode] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');

    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [dateOfBirth, setDateOfBirth] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const savedToken = localStorage.getItem('token');
        const savedUsername = localStorage.getItem('username');
        const savedFullName = localStorage.getItem('fullName');

        if (savedToken && savedUsername && savedFullName) {
            setUser({ username: savedUsername, fullName: savedFullName, token: savedToken });
        }
    }, []);

    useEffect(() => {
        if (user) {
            loadPatients();
        }
    }, [user]);

    const loadPatients = async () => {
        if (!user) return;

        try {
            const response = await fetch(`${API_URL}/api/patients`, {
                headers: {
                    'Authorization': `Bearer ${user.token}`
                }
            });
            const data = await response.json();
            setPatients(data);
        } catch (error) {
            console.error('Ошибка загрузки пациентов:', error);
        }
    };

    const handleAuth = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            const endpoint = loginMode ? '/api/auth/login' : '/api/auth/register';
            const body = loginMode
                ? { username, password }
                : { username, password, fullName };

            const response = await fetch(`${API_URL}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            if (response.ok) {
                const data = await response.json();
                const userData: User = {
                    username: data.username,
                    fullName: data.fullName,
                    token: data.token
                };

                setUser(userData);
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', data.username);
                localStorage.setItem('fullName', data.fullName);

                alert(loginMode ? 'Успешный вход!' : 'Регистрация успешна!');
            } else {
                const error = await response.json();
                alert(error.error || 'Ошибка аутентификации');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка соединения с сервером');
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        setUser(null);
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('fullName');
        setPatients([]);
    };

    const handleAddPatient = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!user) return;

        setLoading(true);

        try {
            const response = await fetch(`${API_URL}/api/patients`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${user.token}`
                },
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
        if (!user || !confirm('Вы уверены, что хотите удалить пациента?')) {
            return;
        }

        try {
            const response = await fetch(`${API_URL}/api/patients/${id}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${user.token}`
                }
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

    if (!user) {
        return (
            <div style={{
                padding: '20px',
                maxWidth: '400px',
                margin: '100px auto',
                border: '1px solid #ccc',
                borderRadius: '8px',
                backgroundColor: '#f9f9f9'
            }}>
                <h1 style={{ textAlign: 'center' }}>
                    {loginMode ? 'Вход в систему' : 'Регистрация'}
                </h1>

                <form onSubmit={handleAuth}>
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>
                            Имя пользователя:
                        </label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            style={{
                                width: '100%',
                                padding: '8px',
                                boxSizing: 'border-box'
                            }}
                        />
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>
                            Пароль:
                        </label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            style={{
                                width: '100%',
                                padding: '8px',
                                boxSizing: 'border-box'
                            }}
                        />
                    </div>

                    {!loginMode && (
                        <div style={{ marginBottom: '15px' }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>
                                ФИО:
                            </label>
                            <input
                                type="text"
                                value={fullName}
                                onChange={(e) => setFullName(e.target.value)}
                                required={!loginMode}
                                style={{
                                    width: '100%',
                                    padding: '8px',
                                    boxSizing: 'border-box'
                                }}
                            />
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            width: '100%',
                            padding: '10px',
                            backgroundColor: '#4CAF50',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            marginBottom: '10px'
                        }}
                    >
                        {loading ? 'Обработка...' : (loginMode ? 'Войти' : 'Зарегистрироваться')}
                    </button>
                </form>

                <button
                    onClick={() => setLoginMode(!loginMode)}
                    style={{
                        width: '100%',
                        padding: '10px',
                        backgroundColor: '#2196F3',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                    }}
                >
                    {loginMode ? 'Нет аккаунта? Регистрация' : 'Есть аккаунт? Войти'}
                </button>
            </div>
        );
    }

    return (
        <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '20px'
            }}>
                <h1>Регистратура</h1>
                <div>
                    <span style={{ marginRight: '15px' }}>👤 {user.fullName}</span>
                    <button
                        onClick={handleLogout}
                        style={{
                            padding: '8px 15px',
                            backgroundColor: '#f44336',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                        }}
                    >
                        Выйти
                    </button>
                </div>
            </div>

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