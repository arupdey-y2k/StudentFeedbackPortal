import React, { useState } from 'react';
import { PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer } from 'recharts';

// Main App Component
function App() {
    const [file, setFile] = useState(null);
    const [fileName, setFileName] = useState('');
    const [captcha, setCaptcha] = useState('');
    const [analyticsData, setAnalyticsData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    // Handler for file input change
    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile) {
            setFile(selectedFile);
            setFileName(selectedFile.name);
            setMessage(''); // Clear previous messages
        } else {
            setFile(null);
            setFileName('');
        }
    };

    // Handler for form submission
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!file) {
            setMessage('Please select a file to upload.');
            return;
        }
        if (captcha !== '12345') {
            setMessage('Invalid CAPTCHA code.');
            return;
        }

        setLoading(true);
        setMessage('');
        setAnalyticsData(null);

        const formData = new FormData();
        formData.append('file', file);
        formData.append('captcha', captcha);

        try {
            // The 'proxy' in package.json will route this to http://localhost:8080
            const response = await fetch('/api/data/upload', {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                // Get error message from the server (e.g., from GlobalExceptionHandler)
                const errorText = await response.text();
                let errorMsg = errorText;
                try {
                    // Try to parse if the error is JSON (like from my exception handler)
                    const errorJson = JSON.parse(errorText);
                    errorMsg = errorJson.error || "An unknown error occurred.";
                } catch (e) {
                    // Not JSON, just use the text
                }
                throw new Error(errorMsg || `Upload failed with status: ${response.status}`);
            }

            // We expect the analytics JSON string as the response body
            const rawAnalytics = await response.text();

            // Attempt to parse the raw string into a JSON object
            try {
                const parsedData = JSON.parse(rawAnalytics);

                // Handle cases where the LLM returns nested data
                if (parsedData.candidates && parsedData.candidates[0].content) {
                    const llmContent = JSON.parse(parsedData.candidates[0].content.parts[0].text);
                    setAnalyticsData(llmContent);
                } else {
                    // Assume direct JSON as in MOCK or already parsed
                    setAnalyticsData(parsedData);
                }

                setMessage('Analysis complete!');
            } catch (parseError) {
                console.error("Failed to parse analytics JSON:", parseError);
                console.error("Raw response was:", rawAnalytics);
                throw new Error('Failed to parse analytics response.');
            }

            // Clear form on success
            setFile(null);
            setFileName('');
            setCaptcha('');

        } catch (error) {
            console.error('Upload error:', error);
            setMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="app-root">
            <div className="frame">
                {/* --- 1. TOP INPUT SECTION (Header Panel) --- */}
                <header className="panel">
                    <h1 style={{textAlign:'center'}} className="title">Student Feedback Analytics Portal</h1>
                </header>

                {/* --- 2. Form Panel: File + CAPTCHA --- */}
                <section className="panel">
                    <form id="uploadForm" onSubmit={handleSubmit} className="grid-2">
                        {/* File Input Column */}
                        <div style={{width:'100%'}}>
                            <label htmlFor="file-upload" className="btn btn-blue">Choose File</label>
                            <input
                                id="file-upload"
                                type="file"
                                accept=".csv"
                                onChange={handleFileChange}
                                style={{display:'none'}}
                            />
                            <div className="muted" title={fileName} style={{marginTop:8}}>
                                {fileName || 'No file selected...'}
                            </div>
                        </div>

                        {/* CAPTCHA Column */}
                        <div style={{width:'100%', display:'flex', flexDirection:'column', alignItems:'center'}}>
                            <div className="captcha-box">1 2 3 4 5</div>
                            <input
                                type="text"
                                value={captcha}
                                onChange={(e) => setCaptcha(e.target.value)}
                                placeholder="Enter CAPTCHA"
                                className="input"
                                style={{marginTop:8, maxWidth:320, width:'100%'}}
                            />
                        </div>
                    </form>
                </section>

                {/* --- 3. Actions Panel: Centered Button + Message --- */}
                <section className="panel" style={{display:'flex', justifyContent:'center', alignItems:'center', flexDirection:'column', gap:12}}>
                    <button
                        type="submit"
                        form="uploadForm"
                        disabled={loading}
                        className="btn btn-green"
                    >
                        {loading ? 'Analyzing...' : 'Upload & Analyze'}
                    </button>
                    {message && (
                        <div className={`message ${analyticsData ? 'msg-success' : 'msg-error'}`}>{message}</div>
                    )}
                </section>

                {/* --- 4. Results Panel --- */}
                <main className="panel results">
                    {loading && <LoadingSpinner />}
                    {!loading && !analyticsData && <Placeholder />}
                    {analyticsData && <AnalyticsDisplay data={analyticsData} />}
                </main>
            </div>
        </div>
    );
}

// --- Helper Components ---

// Loading Spinner
const LoadingSpinner = () => (
    <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-blue-500"></div>
        <span className="ml-4 text-xl font-medium text-gray-600">Analyzing data...</span>
    </div>
);

// Placeholder when no data is loaded
const Placeholder = () => (
    <div className="text-center text-gray-500 p-10 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-semibold mb-2">Welcome!</h2>
        <p className="text-lg">Upload a CSV file to begin your analysis.</p>
        <p>Results will be displayed here.</p>
    </div>
);

// Component to render the analytics
const AnalyticsDisplay = ({ data }) => {
    if (!data || !data.sentiment || !data.keyThemes) {
        return <div className="text-center text-red-500 p-10 bg-white rounded-lg shadow-md">
            <h2 className="text-2xl font-semibold mb-2">Error</h2>
            <p className="text-lg">The analytics data is in an unexpected format.</p>
        </div>
    }

    // Format data for the Pie Chart
    const sentimentData = [
        { name: 'Positive', value: data.sentiment.positive || 0 },
        { name: 'Negative', value: data.sentiment.negative || 0 },
        { name: 'Neutral', value: data.sentiment.neutral || 0 },
    ];

    const COLORS = {
        'Positive': '#10B981', // Green
        'Negative': '#EF4444', // Red
        'Neutral': '#F59E0B',  // Amber
    };

    return (
        <div className="space-y-6">
            {/* Sentiment Card */}
            <section className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-2xl font-bold text-gray-700 mb-4 border-b pb-2">
                    Overall Sentiment
                </h2>
                <div style={{ width: '100%', height: 300 }}>
                    <ResponsiveContainer>
                        <PieChart>
                            <Pie
                                data={sentimentData}
                                cx="50%"
                                cy="50%"
                                outerRadius={100}
                                fill="#8884d8"
                                dataKey="value"
                                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                            >
                                {sentimentData.map((entry) => (
                                    <Cell key={`cell-${entry.name}`} fill={COLORS[entry.name]} />
                                ))}
                            </Pie>
                            <Tooltip formatter={(value, name) => [value, name]} />
                            <Legend />
                        </PieChart>
                    </ResponsiveContainer>
                </div>
            </section>

            {/* Key Themes Card */}
            <section className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-2xl font-bold text-gray-700 mb-4 border-b pb-2">
                    Key Themes
                </h2>
                <ul className="space-y-4">
                    {data.keyThemes.map((theme, index) => (
                        <li key={index} className="bg-gray-50 border border-gray-200 rounded-lg p-4 shadow-sm">
                            <div className="flex justify-between items-center mb-2">
                                <h3 className="text-lg font-semibold text-blue-700">{theme.theme}</h3>
                                <span className="text-sm font-medium bg-blue-100 text-blue-800 px-3 py-1 rounded-full">
                  {theme.mentions} Mentions
                </span>
                            </div>
                            <blockquote className="border-l-4 border-gray-300 pl-4">
                                <p className="text-gray-600 italic">"{theme.exampleQuote}"</p>
                            </blockquote>
                        </li>
                    ))}
                </ul>
            </section>
        </div>
    );
};

export default App;

