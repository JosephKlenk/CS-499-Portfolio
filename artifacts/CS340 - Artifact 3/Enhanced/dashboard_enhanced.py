# Animal Shelter Dashboard
# Joseph Klenk - CS 340 Enhanced Version

import dash
from dash import dcc, html, dash_table
from dash.dependencies import Input, Output
import plotly.express as px
import pandas as pd
from datetime import datetime

print("Starting Animal Shelter Dashboard...")

# Load data from CSV file
try:
    df = pd.read_csv('aac_shelter_outcomes.csv')
    print(f"Loaded {len(df)} records from CSV file")
    
    # Clean the data
    df = df.dropna(subset=['location_lat', 'location_long'])
    df['age_upon_outcome_in_weeks'] = pd.to_numeric(df['age_upon_outcome_in_weeks'], errors='coerce')
    df = df.dropna(subset=['age_upon_outcome_in_weeks'])
    
    print(f"After cleaning: {len(df)} records with complete data")
    
except FileNotFoundError:
    print("CSV file not found! Make sure 'aac_shelter_outcomes.csv' is in the same folder")
    print("Creating sample data for demonstration...")
    
    # Create sample data if CSV is not found
    df = pd.DataFrame({
        'animal_id': [f'A{i:03d}' for i in range(1, 21)],
        'name': ['Buddy', 'Max', 'Luna', 'Bella', 'Charlie', 'Lucy', 'Cooper', 'Daisy', 'Rocky', 'Molly',
                'Zeus', 'Sadie', 'Duke', 'Ruby', 'Bear', 'Zoe', 'Tucker', 'Lily', 'Oscar', 'Chloe'],
        'breed': ['Labrador Retriever Mix', 'German Shepherd', 'Golden Retriever', 'Chesapeake Bay Retriever', 
                 'Siberian Husky', 'Labrador Retriever Mix', 'Rottweiler', 'Golden Retriever', 
                 'German Shepherd', 'Newfoundland'] * 2,
        'color': ['Brown', 'Black', 'Golden', 'Brown', 'White', 'Yellow', 'Black', 'Golden', 
                 'Black', 'Black'] * 2,
        'sex_upon_outcome': ['Intact Male', 'Intact Female'] * 10,
        'age_upon_outcome_in_weeks': [52, 78, 104, 45, 89, 67, 123, 34, 156, 98, 
                                     71, 134, 23, 187, 56, 89, 145, 67, 234, 45],
        'location_lat': [30.75 + i*0.01 for i in range(20)],
        'location_long': [-97.48 - i*0.01 for i in range(20)],
        'outcome_type': ['Adoption', 'Transfer', 'Adoption', 'Adoption', 'Transfer'] * 4,
        'animal_type': ['Dog'] * 20
    })

# Dashboard App - Using regular Dash instead of JupyterDash
app = dash.Dash(__name__)

# Layout
app.layout = html.Div([
    # Header
    html.Div([
        html.H1('Enhanced Animal Shelter Dashboard', 
                style={'text-align': 'center', 'color': '#2c3e50', 'margin': '0'}),
        html.P(f'Analyzing {len(df)} animals from shelter data', 
               style={'text-align': 'center', 'color': '#7f8c8d', 'margin': '5px 0'}),
        html.P('Created by Joseph Klenk - CS 340 Enhanced Version', 
               style={'text-align': 'center', 'color': '#95a5a6', 'font-size': '12px', 'margin': '0'})
    ], style={
        'padding': '20px', 
        'background': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'color': 'white',
        'border-radius': '10px', 
        'margin-bottom': '20px',
        'box-shadow': '0 4px 6px rgba(0,0,0,0.1)'
    }),
    
    # Statistics Cards
    html.Div([
        html.Div([
            html.H2(f"{len(df):,}", style={'margin': '0', 'color': '#3498db', 'font-size': '2em'}),
            html.P('Total Animals', style={'margin': '5px 0', 'font-weight': 'bold'})
        ], className='stat-card'),
        
        html.Div([
            html.H2(f"{df['breed'].nunique()}", style={'margin': '0', 'color': '#e74c3c', 'font-size': '2em'}),
            html.P('Unique Breeds', style={'margin': '5px 0', 'font-weight': 'bold'})
        ], className='stat-card'),
        
        html.Div([
            html.H2(f"{df['age_upon_outcome_in_weeks'].mean():.0f}", style={'margin': '0', 'color': '#2ecc71', 'font-size': '2em'}),
            html.P('Avg Age (weeks)', style={'margin': '5px 0', 'font-weight': 'bold'})
        ], className='stat-card'),
        
        html.Div([
            html.H2(datetime.now().strftime('%H:%M'), style={'margin': '0', 'color': '#f39c12', 'font-size': '2em'}),
            html.P('Last Updated', style={'margin': '5px 0', 'font-weight': 'bold'})
        ], className='stat-card')
    ], style={
        'display': 'flex', 
        'justify-content': 'space-around', 
        'margin-bottom': '30px',
        'flex-wrap': 'wrap'
    }),
    
    # Filter Controls
    html.Div([
        html.H3('🔍 Filter Controls', style={'margin-bottom': '20px', 'color': '#2c3e50'}),
        
        html.Div([
            # Rescue Type Filter
            html.Div([
                html.Label('Rescue Type:', style={'font-weight': 'bold', 'margin-bottom': '10px', 'display': 'block'}),
                dcc.RadioItems(
                    id='filter-type',
                    options=[
                        {'label': 'Water Rescue', 'value': 'water'},
                        {'label': 'Mountain/Wilderness Rescue', 'value': 'mountain'},
                        {'label': 'Disaster Rescue/Tracking', 'value': 'disaster'},
                        {'label': 'Show All Animals', 'value': 'reset'}
                    ],
                    value='reset',
                    labelStyle={'display': 'block', 'margin': '8px 0', 'font-size': '14px'}
                )
            ], style={'width': '45%', 'display': 'inline-block', 'vertical-align': 'top'}),
            
            # Age Range Filter
            html.Div([
                html.Label('Age Range (weeks):', style={'font-weight': 'bold', 'margin-bottom': '10px', 'display': 'block'}),
                dcc.RangeSlider(
                    id='age-range-slider',
                    min=0,
                    max=int(df['age_upon_outcome_in_weeks'].max()) + 20,
                    step=10,
                    marks={i: f'{i}w' for i in range(0, int(df['age_upon_outcome_in_weeks'].max()) + 20, 50)},
                    value=[0, int(df['age_upon_outcome_in_weeks'].max()) + 20],
                    tooltip={"placement": "bottom", "always_visible": True}
                ),
                html.Div(id='age-range-output', style={'margin-top': '10px', 'font-size': '12px', 'color': '#666'})
            ], style={'width': '45%', 'float': 'right'})
        ])
    ], style={
        'padding': '20px', 
        'background-color': '#f8f9fa', 
        'border-radius': '10px', 
        'margin-bottom': '20px',
        'box-shadow': '0 2px 4px rgba(0,0,0,0.1)'
    }),
    
    # Results Summary
    html.Div(id='results-summary', style={'margin-bottom': '20px'}),
    
    # Charts Section
    html.Div([
        html.H3('Analytics Dashboard', style={'margin-bottom': '20px', 'color': '#2c3e50'}),
        
        html.Div([
            # Breed Chart
            html.Div([
                dcc.Graph(id='breed-chart')
            ], style={'width': '48%', 'display': 'inline-block'}),
            
            # Age Distribution Chart
            html.Div([
                dcc.Graph(id='age-chart')
            ], style={'width': '48%', 'float': 'right'})
        ]),
        
        # Map Chart
        html.Div([
            dcc.Graph(id='map-chart')
        ], style={'margin-top': '20px'})
    ]),
    
    # Data Table
    html.Div([
        html.H3('Animal Data Table', style={'margin-bottom': '15px', 'color': '#2c3e50'}),
        dash_table.DataTable(
            id='datatable-id',
            columns=[{"name": i.replace('_', ' ').title(), "id": i} for i in df.columns[:8]],  # Show first 8 columns
            data=df.to_dict('records'),
            page_size=10,
            page_action='native',
            sort_action='native',
            sort_mode='multi',
            filter_action='native',
            style_table={'overflowX': 'auto'},
            style_cell={
                'height': 'auto',
                'minWidth': '100px', 
                'width': '120px', 
                'maxWidth': '200px',
                'whiteSpace': 'normal',
                'textAlign': 'left',
                'padding': '8px',
                'fontFamily': 'Arial, sans-serif',
                'fontSize': '13px'
            },
            style_header={
                'backgroundColor': '#3498db',
                'color': 'white',
                'fontWeight': 'bold',
                'textAlign': 'center'
            },
            style_data_conditional=[
                {
                    'if': {'row_index': 'odd'},
                    'backgroundColor': '#f8f9fa'
                }
            ]
        )
    ], style={'margin-top': '30px'})
    
], style={
    'margin': '20px', 
    'fontFamily': 'Arial, sans-serif',
    'backgroundColor': '#ffffff'
})

# Add CSS
app.index_string = '''
<!DOCTYPE html>
<html>
    <head>
        {%metas%}
        <title>Animal Shelter Dashboard</title>
        {%favicon%}
        {%css%}
        <style>
            .stat-card {
                background: white;
                padding: 20px;
                border-radius: 10px;
                text-align: center;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                margin: 10px;
                flex: 1;
                min-width: 150px;
                border-left: 4px solid #3498db;
            }
            body {
                background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                margin: 0;
                padding: 0;
            }
        </style>
    </head>
    <body>
        {%app_entry%}
        <footer>
            {%config%}
            {%scripts%}
            {%renderer%}
        </footer>
    </body>
</html>
'''

#############################################
# Callbacks
#############################################

@app.callback(
    [Output('datatable-id', 'data'),
     Output('results-summary', 'children'),
     Output('age-range-output', 'children')],
    [Input('filter-type', 'value'),
     Input('age-range-slider', 'value')]
)
def update_data(filter_type, age_range):
    """Filter data based on rescue type and age range"""
    filtered_df = df.copy()
    
    # Apply age filter
    filtered_df = filtered_df[
        (filtered_df['age_upon_outcome_in_weeks'] >= age_range[0]) &
        (filtered_df['age_upon_outcome_in_weeks'] <= age_range[1])
    ]
    
    # Apply rescue type filter
    if filter_type == 'water':
        filtered_df = filtered_df[
            (filtered_df['breed'].str.contains('Labrador Retriever Mix|Chesapeake Bay Retriever|Newfoundland', case=False, na=False)) &
            (filtered_df['sex_upon_outcome'] == 'Intact Female') &
            (filtered_df['age_upon_outcome_in_weeks'] >= 26) &
            (filtered_df['age_upon_outcome_in_weeks'] <= 156)
        ]
        filter_desc = "Water Rescue (Female, 26-156 weeks, specific breeds)"
    elif filter_type == 'mountain':
        filtered_df = filtered_df[
            (filtered_df['breed'].str.contains('German Shepherd|Alaskan Malamute|Old English Sheepdog|Siberian Husky|Rottweiler', case=False, na=False)) &
            (filtered_df['sex_upon_outcome'] == 'Intact Male') &
            (filtered_df['age_upon_outcome_in_weeks'] >= 26) &
            (filtered_df['age_upon_outcome_in_weeks'] <= 156)
        ]
        filter_desc = "Mountain/Wilderness Rescue (Male, 26-156 weeks, specific breeds)"
    elif filter_type == 'disaster':
        filtered_df = filtered_df[
            (filtered_df['breed'].str.contains('Doberman Pinscher|German Shepherd|Golden Retriever|Bloodhound|Rottweiler', case=False, na=False)) &
            (filtered_df['sex_upon_outcome'] == 'Intact Male') &
            (filtered_df['age_upon_outcome_in_weeks'] >= 20) &
            (filtered_df['age_upon_outcome_in_weeks'] <= 300)
        ]
        filter_desc = "Disaster/Tracking Rescue (Male, 20-300 weeks, specific breeds)"
    else:
        filter_desc = "All animals"
    
    # Results summary
    summary = html.Div([
        html.H4(f"🎯 Results: {len(filtered_df)} animals found", style={'color': '#2c3e50', 'margin': '0'}),
        html.P(f"Filter: {filter_desc}", style={'color': '#7f8c8d', 'margin': '5px 0'})
    ], style={
        'padding': '15px',
        'background': 'white',
        'border-radius': '5px',
        'border-left': '4px solid #2ecc71'
    })
    
    # Age range output
    age_output = f"Selected range: {age_range[0]} - {age_range[1]} weeks"
    
    return filtered_df.to_dict('records'), summary, age_output

@app.callback(
    Output('breed-chart', 'figure'),
    [Input('datatable-id', 'data')]
)
def update_breed_chart(table_data):
    """Update breed distribution chart"""
    if not table_data:
        return px.pie(title="No data available")
    
    chart_df = pd.DataFrame(table_data)
    breed_counts = chart_df['breed'].value_counts().head(8)
    
    fig = px.pie(
        names=breed_counts.index,
        values=breed_counts.values,
        title=f'Top Breeds ({len(chart_df)} animals)'
    )
    
    fig.update_layout(
        height=400,
        title_x=0.5,
        font=dict(size=12)
    )
    return fig

@app.callback(
    Output('age-chart', 'figure'),
    [Input('datatable-id', 'data')]
)
def update_age_chart(table_data):
    """Update age distribution chart"""
    if not table_data:
        return px.histogram(title="No data available")
    
    chart_df = pd.DataFrame(table_data)
    
    fig = px.histogram(
        chart_df,
        x='age_upon_outcome_in_weeks',
        nbins=15,
        title=f'Age Distribution ({len(chart_df)} animals)',
        labels={'age_upon_outcome_in_weeks': 'Age (weeks)', 'count': 'Number of Animals'}
    )
    
    fig.update_layout(
        height=400,
        title_x=0.5,
        xaxis_title="Age (weeks)",
        yaxis_title="Count"
    )
    return fig

@app.callback(
    Output('map-chart', 'figure'),
    [Input('datatable-id', 'data')]
)
def update_map(table_data):
    """Update map visualization"""
    if not table_data:
        return px.scatter_mapbox(title="No data available")
    
    map_df = pd.DataFrame(table_data)
    
    if len(map_df) == 0:
        return px.scatter_mapbox(title="No animals match current filters")
    
    fig = px.scatter_mapbox(
        map_df.head(100),  # Limit to 100 points for performance
        lat='location_lat',
        lon='location_long',
        hover_name='name',
        hover_data=['breed', 'age_upon_outcome_in_weeks'],
        color='breed',
        zoom=9,
        height=500,
        title=f'🗺️ Animal Locations (showing {min(100, len(map_df))} of {len(map_df)})'
    )
    
    fig.update_layout(
        mapbox_style="open-street-map",
        mapbox=dict(center=dict(lat=30.75, lon=-97.48)),
        margin={"r":0,"t":50,"l":0,"b":0},
        title_x=0.5
    )
    
    return fig

# Run the app
if __name__ == '__main__':
    print("\n" + "="*50)
    print("DASHBOARD STARTING...")
    print("Open your browser to: http://127.0.0.1:8050/")
    print("The dashboard will auto-reload when you make changes")
    print("Press Ctrl+C to stop the server")
    print("="*50 + "\n")
    
app.run(debug=True, port=8050, host='127.0.0.1')
