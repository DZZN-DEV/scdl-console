import subprocess
import sys
from typing import List
import scdl

class ScdlCommand:
    def run_command(self, args: List[str]) -> None:
        """Run a command with the provided arguments."""
        try:
            result = subprocess.run([sys.executable, '-m', 'scdl'] + args, check=True, capture_output=True, text=True)
            print(result.stdout)
        except subprocess.CalledProcessError as e:
            print(e.stderr, file=sys.stderr)
            sys.exit(e.returncode)

    def download_url(self, url: str, **kwargs) -> None:
        args = ['-l', url]
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def show_help(self) -> None:
        self.run_command(['-h'])

    def show_version(self) -> None:
        self.run_command(['--version'])

    def download_me(self, **kwargs) -> None:
        args = ['me']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_all_tracks(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-a']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_favorites(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-f']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_commented(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-C']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_uploads(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-t']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_playlists(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-p']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def download_reposts(self, url: str, **kwargs) -> None:
        args = ['-l', url, '-r']
        args.extend(self._build_args_from_kwargs(kwargs))
        self.run_command(args)

    def _build_args_from_kwargs(self, kwargs: dict) -> List[str]:
        args = []
        for key, value in kwargs.items():
            if isinstance(value, bool):
                if value:
                    args.append(f'--{key.replace("_", "-")}')
            else:
                args.append(f'--{key.replace("_", "-")}')
                args.append(str(value))
        return args
