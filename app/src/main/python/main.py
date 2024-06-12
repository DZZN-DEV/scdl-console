import scdl
import sys
import os
import json

def download(url, max_tracks=None, download_favorites=False, auth_token=None, download_path=None):
    response = {"success": False, "error": ""}
    
    if not url:
        response["error"] = "URL is required."
        return json.dumps(response)
    
    # Define the download path (use provided path or default to internal storage 'scdl' folder)
    if not download_path:
        download_path = os.path.join(os.getenv('HOME'), 'scdl')
    
    # Ensure the directory exists
    if not os.path.exists(download_path):
        os.makedirs(download_path)
    
    # Prepare scdl arguments
    args = ['-l', url, '--path', download_path]

    if max_tracks:
        args.extend(['-n', str(max_tracks)])
    if download_favorites:
        args.append('-f')
    if auth_token:
        args.extend(['--auth-token', auth_token])

    try:
        scdl.main(args)
        response["success"] = True
    except Exception as e:
        response["error"] = str(e)
    
    return json.dumps(response)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: main.py <url> [max_tracks] [download_favorites] [auth_token] [download_path]")
    else:
        url = sys.argv[1]
        max_tracks = sys.argv[2] if len(sys.argv) > 2 else None
        download_favorites = sys.argv[3].lower() == 'true' if len(sys.argv) > 3 else False
        auth_token = sys.argv[4] if len(sys.argv) > 4 else None
        download_path = sys.argv[5] if len(sys.argv) > 5 else None
        print(download(url, max_tracks, download_favorites, auth_token, download_path))
