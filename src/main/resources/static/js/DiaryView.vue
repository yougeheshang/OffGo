async rateDiary(diaryId, rating) {
  try {
    console.log('Sending rating request:', { diaryId, userId: this.userId, rating });
    const response = await fetch('http://localhost:8050/diary/rate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        diaryId: diaryId,
        userId: this.userId,
        rating: rating
      })
    });

    if (!response.ok) {
      const errorData = await response.text();
      console.error('Rating request failed:', response.status, errorData);
      throw new Error(`评分失败: ${errorData}`);
    }

    const data = await response.json();
    console.log('Rating response:', data);
    return data;
  } catch (error) {
    console.error('Error rating diary:', error);
    throw error;
  }
} 