import React, { useState, useEffect, useRef } from 'react';
import { Users, Play, Folder, File, Plus, Terminal, Save, Download, Settings, Moon, Sun } from 'lucide-react';

const CollaborativeCodeEditor = () => {
  const [code, setCode] = useState('// Write your code here...\n');
  const [output, setOutput] = useState('');
  const [language, setLanguage] = useState('javascript');
  const [roomId, setRoomId] = useState('');
  const [username, setUsername] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [activeUsers, setActiveUsers] = useState([]);
  const [files, setFiles] = useState([
    { id: 1, name: 'main.js', type: 'file', content: '// Write your code here...\n', language: 'javascript' }
  ]);
  const [activeFileId, setActiveFileId] = useState(1);
  const [isRunning, setIsRunning] = useState(false);
  const [theme, setTheme] = useState('dark');
  const [showJoinModal, setShowJoinModal] = useState(true);
  
  const wsRef = useRef(null);
  const editorRef = useRef(null);

  // Language configurations
  const languages = [
    { value: 'javascript', label: 'JavaScript', ext: '.js' },
    { value: 'python', label: 'Python', ext: '.py' },
    { value: 'java', label: 'Java', ext: '.java' },
    { value: 'cpp', label: 'C++', ext: '.cpp' },
    { value: 'html', label: 'HTML', ext: '.html' },
    { value: 'css', label: 'CSS', ext: '.css' }
  ];

  // User colors for collaborative cursors
  const userColors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', 
    '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E2'
  ];

  // Connect to WebSocket
  const connectToRoom = () => {
    if (!roomId || !username) {
      alert('Please enter both room ID and username');
      return;
    }

    const ws = new WebSocket(`ws://localhost:8080/ws/code?roomId=${roomId}&username=${username}`);
    
    ws.onopen = () => {
      console.log('WebSocket Connected');
      setIsConnected(true);
      setShowJoinModal(false);
    };

    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      handleWebSocketMessage(message);
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    ws.onclose = () => {
      console.log('WebSocket Disconnected');
      setIsConnected(false);
    };

    wsRef.current = ws;
  };

  // Handle incoming WebSocket messages
  const handleWebSocketMessage = (message) => {
    switch(message.type) {
      case 'USER_JOINED':
        setActiveUsers(prev => [...prev, { id: message.userId, name: message.username, color: userColors[prev.length % userColors.length] }]);
        break;
      case 'USER_LEFT':
        setActiveUsers(prev => prev.filter(u => u.id !== message.userId));
        break;
      case 'CODE_UPDATE':
        // Apply CRDT operation
        setCode(message.content);
        break;
      case 'FILE_CHANGE':
        setFiles(message.files);
        break;
      case 'USERS_LIST':
        setActiveUsers(message.users.map((u, i) => ({
          ...u,
          color: userColors[i % userColors.length]
        })));
        break;
      default:
        break;
    }
  };

  // Send code changes
  const handleCodeChange = (e) => {
    const newCode = e.target.value;
    setCode(newCode);
    
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'CODE_CHANGE',
        content: newCode,
        fileId: activeFileId,
        roomId: roomId
      }));
    }
  };

  // Run code
  const runCode = async () => {
    setIsRunning(true);
    setOutput('Running...');
    
    try {
      const response = await fetch('http://localhost:8080/api/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: code,
          language: language,
          roomId: roomId
        })
      });
      
      const result = await response.json();
      setOutput(result.output || result.error || 'Execution completed');
    } catch (error) {
      setOutput(`Error: ${error.message}`);
    } finally {
      setIsRunning(false);
    }
  };

  // Create new file
  const createNewFile = () => {
    const newFile = {
      id: Date.now(),
      name: `untitled${files.length}.${languages.find(l => l.value === language)?.ext || '.txt'}`,
      type: 'file',
      content: '',
      language: language
    };
    setFiles([...files, newFile]);
    setActiveFileId(newFile.id);
    
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'FILE_CREATE',
        file: newFile,
        roomId: roomId
      }));
    }
  };

  // Switch file
  const switchFile = (fileId) => {
    setActiveFileId(fileId);
    const file = files.find(f => f.id === fileId);
    if (file) {
      setCode(file.content);
      setLanguage(file.language);
    }
  };

  // Save file
  const saveFile = () => {
    const file = files.find(f => f.id === activeFileId);
    if (file) {
      file.content = code;
      setFiles([...files]);
      
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({
          type: 'FILE_SAVE',
          fileId: activeFileId,
          content: code,
          roomId: roomId
        }));
      }
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  const bgColor = theme === 'dark' ? 'bg-gray-900' : 'bg-gray-50';
  const textColor = theme === 'dark' ? 'text-gray-100' : 'text-gray-900';
  const editorBg = theme === 'dark' ? 'bg-gray-800' : 'bg-white';
  const sidebarBg = theme === 'dark' ? 'bg-gray-800' : 'bg-gray-100';

  return (
    <div className={`h-screen flex flex-col ${bgColor} ${textColor}`}>
      {/* Join Room Modal */}
      {showJoinModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className={`${editorBg} p-8 rounded-lg shadow-2xl w-96`}>
            <h2 className="text-2xl font-bold mb-6">Join Collaborative Session</h2>
            <input
              type="text"
              placeholder="Room ID"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              className={`w-full px-4 py-2 mb-4 rounded border ${theme === 'dark' ? 'bg-gray-700 border-gray-600' : 'bg-white border-gray-300'}`}
            />
            <input
              type="text"
              placeholder="Your Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={`w-full px-4 py-2 mb-6 rounded border ${theme === 'dark' ? 'bg-gray-700 border-gray-600' : 'bg-white border-gray-300'}`}
            />
            <button
              onClick={connectToRoom}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded font-semibold transition"
            >
              Join Room
            </button>
          </div>
        </div>
      )}

      {/* Top Bar */}
      <div className={`${sidebarBg} border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'} px-4 py-2 flex items-center justify-between`}>
        <div className="flex items-center gap-4">
          <h1 className="text-xl font-bold">CodeCollab</h1>
          <div className="flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <span className="text-sm">{isConnected ? `Room: ${roomId}` : 'Disconnected'}</span>
          </div>
        </div>
        
        <div className="flex items-center gap-4">
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className={`px-3 py-1 rounded ${theme === 'dark' ? 'bg-gray-700' : 'bg-white'}`}
          >
            {languages.map(lang => (
              <option key={lang.value} value={lang.value}>{lang.label}</option>
            ))}
          </select>
          
          <button
            onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            className="p-2 hover:bg-gray-700 rounded"
          >
            {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
          </button>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar - File Explorer */}
        <div className={`w-64 ${sidebarBg} border-r ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'} flex flex-col`}>
          <div className="p-4 border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'}">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold flex items-center gap-2">
                <Folder size={18} />
                Files
              </h3>
              <button
                onClick={createNewFile}
                className="p-1 hover:bg-gray-700 rounded"
              >
                <Plus size={18} />
              </button>
            </div>
          </div>
          
          <div className="flex-1 overflow-y-auto p-2">
            {files.map(file => (
              <div
                key={file.id}
                onClick={() => switchFile(file.id)}
                className={`px-3 py-2 rounded cursor-pointer flex items-center gap-2 mb-1 ${
                  activeFileId === file.id 
                    ? 'bg-blue-600 text-white' 
                    : 'hover:bg-gray-700'
                }`}
              >
                <File size={16} />
                <span className="text-sm truncate">{file.name}</span>
              </div>
            ))}
          </div>

          {/* Active Users */}
          <div className={`p-4 border-t ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`}>
            <h3 className="font-semibold flex items-center gap-2 mb-3">
              <Users size={18} />
              Active Users ({activeUsers.length})
            </h3>
            <div className="space-y-2">
              {activeUsers.map(user => (
                <div key={user.id} className="flex items-center gap-2">
                  <div 
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: user.color }}
                  ></div>
                  <span className="text-sm">{user.name}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Main Editor Area */}
        <div className="flex-1 flex flex-col">
          {/* Toolbar */}
          <div className={`${sidebarBg} px-4 py-2 flex items-center gap-2 border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`}>
            <button
              onClick={runCode}
              disabled={isRunning}
              className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded transition"
            >
              <Play size={16} />
              {isRunning ? 'Running...' : 'Run Code'}
            </button>
            <button
              onClick={saveFile}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition"
            >
              <Save size={16} />
              Save
            </button>
          </div>

          {/* Code Editor */}
          <div className="flex-1 overflow-hidden">
            <textarea
              ref={editorRef}
              value={code}
              onChange={handleCodeChange}
              className={`w-full h-full p-4 font-mono text-sm resize-none focus:outline-none ${editorBg} ${textColor}`}
              style={{ 
                fontFamily: 'Monaco, Consolas, "Courier New", monospace',
                lineHeight: '1.6',
                tabSize: 2
              }}
              spellCheck={false}
            />
          </div>
        </div>

        {/* Output Panel */}
        <div className={`w-96 ${sidebarBg} border-l ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'} flex flex-col`}>
          <div className={`px-4 py-2 border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-300'} flex items-center gap-2`}>
            <Terminal size={18} />
            <h3 className="font-semibold">Output</h3>
          </div>
          <div className={`flex-1 p-4 overflow-y-auto font-mono text-sm ${theme === 'dark' ? 'bg-gray-900' : 'bg-gray-50'}`}>
            <pre className="whitespace-pre-wrap">{output || 'Output will appear here...'}</pre>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CollaborativeCodeEditor;