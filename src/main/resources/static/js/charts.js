// RepoInsight Chart Utilities

function renderCoverageChart(data) {
    const canvas = document.getElementById('coverageChart');
    if (!canvas) return;

    const total = (data.covered || 0) + (data.partial || 0) + (data.notCovered || 0) + (data.unclear || 0);
    if (total === 0) {
        drawEmptyChart(canvas, 'No data');
        return;
    }

    const ctx = canvas.getContext('2d');
    const colors = ['#38a169', '#d69e2e', '#e53e3e', '#a0aec0'];
    const values = [data.covered || 0, data.partial || 0, data.notCovered || 0, data.unclear || 0];

    drawDonut(ctx, canvas.width, canvas.height, values, colors, total);
}

function drawDonut(ctx, w, h, values, colors, total) {
    const cx = w / 2, cy = h / 2;
    const radius = Math.min(cx, cy) - 20;
    const inner  = radius * 0.55;

    let angle = -Math.PI / 2;
    values.forEach((val, i) => {
        if (val <= 0) return;
        const slice = (val / total) * 2 * Math.PI;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, radius, angle, angle + slice);
        ctx.closePath();
        ctx.fillStyle = colors[i];
        ctx.fill();
        angle += slice;
    });

    ctx.beginPath();
    ctx.arc(cx, cy, inner, 0, 2 * Math.PI);
    ctx.fillStyle = '#ffffff';
    ctx.fill();

    const pct = Math.round(((values[0] + values[1] * 0.5) / total) * 100);
    ctx.fillStyle = '#2d3748';
    ctx.font = 'bold 24px -apple-system, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(pct + '%', cx, cy);
}

function drawEmptyChart(canvas, msg) {
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#f7fafc';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#a0aec0';
    ctx.font = '14px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(msg, canvas.width / 2, canvas.height / 2);
}
